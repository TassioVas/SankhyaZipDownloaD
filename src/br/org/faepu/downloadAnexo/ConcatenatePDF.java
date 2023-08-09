package br.org.faepu.downloadAnexo;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class ConcatenatePDF {
	
  private Collection pdfFiles = new ArrayList();
  
  private boolean numeration = false;
  
  public ConcatenatePDF() {
  }
  
  public ConcatenatePDF(Collection pdfFiles) {
	  System.out.println("Caiu no concate");
    this.pdfFiles = pdfFiles;
  }
  
  public void setPdfFiles(Collection pdfFiles) {
    this.pdfFiles = pdfFiles;
  }
  
  public int getSize() {
    return this.pdfFiles.size();
  }
  
  public void addPdfFile(byte[] pdf) {
	  System.out.println("Caiu no concate");
    this.pdfFiles.add(pdf);
  }
  
  public boolean isNumeration() {
    return this.numeration;
  }
  
  public void setNumeration(boolean numeration) {
    this.numeration = numeration;
  }
  
  public Collection getPdfFiles() {
    return this.pdfFiles;
  }
  
  /*Esse classe serve para concatenar os aruivos de acordo com a necessidade em 1 so arquivo PDF*/
  
  public ByteArrayOutputStream run() throws DocumentException, IOException, IOException {
    ByteArrayOutputStream pdfConcatenated = new ByteArrayOutputStream();
    int f = 0;
    Document document = null;
    PdfContentByte pdfContentByte = null;
    PdfWriter pdfWriter = null;
    for (Iterator<byte[]> ite = this.pdfFiles.iterator(); ite.hasNext(); ) {
      byte[] bytes = ite.next();
      if (bytes == null)
        throw new IllegalArgumentException("Nexiste PDF em um anexo cadastrado como PDF. Por favor verifique!"); 
      PdfReader reader = new PdfReader(bytes);
      reader.consolidateNamedDestinations();
      if (f == 0) {
        document = new Document(reader.getPageSizeWithRotation(1));
        pdfWriter = PdfWriter.getInstance(document, pdfConcatenated);
        document.open();
        if (isNumeration()) {
          HeaderFooter header = new HeaderFooter(new Phrase("Fls.: "), true);
          header.setAlignment(2);
          header.setBorder(0);
          document.resetHeader();
          document.setHeader(header);
        } 
        pdfContentByte = pdfWriter.getDirectContent();
      } 
      int i = 0;
      while (i < reader.getNumberOfPages()) {
        i++;
        document.setPageSize(reader.getPageSizeWithRotation(i));
        document.newPage();
        PdfImportedPage page = pdfWriter.getImportedPage(reader, i);
        int rotation = reader.getPageRotation(i);
        if (rotation == 90 || rotation == 270) {
          pdfContentByte.addTemplate((PdfTemplate)page, 0.0F, -1.0F, 1.0F, 0.0F, 0.0F, reader.getPageSizeWithRotation(i).getHeight());
          continue;
        } 
        pdfContentByte.addTemplate((PdfTemplate)page, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F);
      } 
      f++;
    } 
    if (document != null)
      document.close(); 
    return pdfConcatenated;
  }
}
