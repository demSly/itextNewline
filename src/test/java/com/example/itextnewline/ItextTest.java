package com.example.itextnewline;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.clipper.ClipperException;
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy;
import com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;

class ItextTest {

  private static final Logger LOG = LoggerFactory.getLogger(ItextTest.class);

  @Test
  void test() throws URISyntaxException, IOException {
    PdfDocument pdfDocument = getPdfDocument(getPdfDocumentFromResources("Mapress.pdf"));
    assertNotNull(pdfDocument);

    PdfPage pdfPage = pdfDocument.getPage(1);
    assertNotNull(pdfPage);

    String textFromPage = getTextFromPage(pdfPage);
    assertNotNull(textFromPage);

    // Parser reads a newline that does not exist. Following text was copied from Mparess.pdf
    assertTrue(textFromPage.contains("Ø18 6 m"));
  }

  public static InputStream getPdfDocumentFromResources(String resourceName)
      throws URISyntaxException, IOException {
    URI uri = ClassLoader.getSystemResource(resourceName).toURI();
    final Path path;
    if (StringUtils.contains(uri.toString(), "!")) {
      final String[] array = uri.toString().split("!");
      FileSystem fs;
      try {
        fs = FileSystems.getFileSystem(URI.create(array[0]));
      } catch (FileSystemNotFoundException e) {
        fs = FileSystems.newFileSystem(URI.create(array[0]), Map.of());
      }
      path = fs.getPath(array[1]);
    } else {
      path = Paths.get(uri);
    }
    FileSystemResource fileSystemResource = new FileSystemResource(path);

    return fileSystemResource.getInputStream();
  }

  public PdfDocument getPdfDocument(InputStream pdfData) {
    try {
      PdfReader pdfReader = new PdfReader(pdfData);
      return new PdfDocument(pdfReader);
    } catch (IOException e) {
      throw new RuntimeException("Exception during PDF Loading");
    }
  }

  public String getTextFromPage(PdfPage pdfPage) {
    String textFromPage;
    try {
      textFromPage = com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
          .getTextFromPage(pdfPage, new LocationTextExtractionStrategy());
    } catch (ClipperException e) {
      int pageNumber = pdfPage.getDocument().getPageNumber(pdfPage);
      LOG.warn("Problem extracting text from PdfPage: {}. Trying alternate strategy",
          pageNumber);
      textFromPage = com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
          .getTextFromPage(pdfPage, new SimpleTextExtractionStrategy());
      if (isEmpty(textFromPage)) {
        LOG.error("Error extracting text from PdfPage: {}", pageNumber, e);
      }
    }
    LOG.debug("textFromPage: {}", textFromPage);
    return textFromPage;
  }
}
