package br.org.faepu.downloadAnexo;


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.sankhya.util.SessionFile;
import com.sankhya.util.UIDGenerator;
import com.sankhya.util.ZipUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.comercial.ImpressaoNotaHelpper;
import br.com.sankhya.modelcore.util.ArquivoModeloUtils;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.Report;
import br.com.sankhya.modelcore.util.ReportManager;
import br.com.sankhya.ws.ServiceContext;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;


public class ExpFatura implements AcaoRotinaJava {
	
	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		
		for (int i = 0; i < ctx.getLinhas().length; i++) {
			Registro line = ctx.getLinhas()[i];
			try {
				exportacaoCTe(ctx, line);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/*Esse codigo e usado para servir de exemplo para poder enviar, para um arquivo zip  e poder fazer download */

	@SuppressWarnings("static-access")
	private void exportacaoCTe(ContextoAcao ctx, Registro line) {

		String fatura = (String) ctx.getParam("FATURA");
		String nota = (String) ctx.getParam("NOTA");
		String xml = (String) ctx.getParam("XML");
		
		if (fatura.equals("N") && nota.equals("N") && xml.equals("N")) {
			ctx.setMensagemRetorno("Falha na exporta��o da fatura. \n Verifique os par�metros.");
			return;
		}
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
		
		try {
		
			jdbc.openSession();
			
			Map<String, byte[]> arquivos = new HashMap<String, byte[]>();
			
			BigDecimal numFatura =  (BigDecimal) line.getCampo("NUMDUPL");
			
			System.out.println("N�mero Fatura: " + numFatura);
			
			BigDecimal nroRelatorio = new BigDecimal(0);
			Map<String, Object> parameters = new HashMap<String, Object>();
			Report modeloImpressao = null;
			JasperPrint jasperPrint = null;
			byte[] arqFatura = null;
			
			if (fatura.equals("S")) {
				
				//inicio gera��o do relat�rio fatura
				nroRelatorio = (BigDecimal) ctx.getParametroSistema("AD_NURFEFATURA");
				
				parameters.put("PDIR_MODELO", ArquivoModeloUtils.getDiretorioModelos());
				parameters.put("P_NUMDUPL", numFatura.toString());
							
				modeloImpressao = ReportManager.getInstance().getReport(nroRelatorio, dwfEntityFacade);
				
				jasperPrint = modeloImpressao.buildJasperPrint(parameters, jdbc.getConnection());
				
				arqFatura = JasperExportManager.exportReportToPdf(jasperPrint);

				arquivos.put(numFatura + "FATURAGLOBAL.pdf",arqFatura);			
				//final gera��o do relat�rio fatura
				
			}
						
			//busca os NUNOTA
			QueryExecutor query = ctx.getQuery();
			
			StringBuffer sqlQuery = new StringBuffer("");
			sqlQuery.append(" SELECT ");
			sqlQuery.append("	CAB.NUNOTA, CAB.NUMNOTA, TP.CODMODDOC, ");
			sqlQuery.append("	ISNULL(CAB.STATUSCTE,'N') AS STATUSCTE, ");
			sqlQuery.append("	ISNULL(CAB.STATUSNFSE,'N') AS STATUSNFSE, ");
			sqlQuery.append("	(SELECT NURFE FROM TGFMON WHERE CODMODNF = ISNULL(NUM.MODNOTAFIS,TP.CODMODNF)) NURFE, ");
			sqlQuery.append("	(SELECT XMLENVCLI AS ARQUIVOXML FROM TGFNCTE WHERE NUNOTA = CAB.NUNOTA) AS ARQUIVOXML ");
			sqlQuery.append(" FROM TGFFIN FIN ");
			sqlQuery.append(" JOIN TGFCAB CAB ON FIN.NUNOTA = CAB.NUNOTA ");
			sqlQuery.append(" JOIN TGFTOP TP ON CAB.CODTIPOPER = TP.CODTIPOPER AND CAB.DHTIPOPER = TP.DHALTER ");
			sqlQuery.append(" LEFT JOIN TGFNUM NUM ");
			sqlQuery.append(" ON ARQUIVO = 'VENDA' AND CAB.SERIENOTA = NUM.SERIE AND CAB.CODEMP = NUM.CODEMP AND TP.CODMODDOC = NUM.CODMODDOC ");
			sqlQuery.append(" WHERE ISNULL(FIN.NUMDUPL,0) = " + numFatura + " ");

			query.nativeSelect(sqlQuery.toString());
			
			while (query.next()) {
				
				BigDecimal nuNota = query.getBigDecimal("NUNOTA");
				
				String statusCte = query.getString("STATUSCTE");
				
				String statusNFSe = query.getString("STATUSNFSE");

				String codModDoc = query.getString("CODMODDOC");
				
				System.out.println("nuNota: " + nuNota);
				System.out.println("statusCte: " + statusCte);
				System.out.println("statusNFSe: " + statusNFSe);
				System.out.println("codModDoc: " + codModDoc);
				
				if (statusCte.equals("A") && codModDoc.equals("57")) {
					
					JapeWrapper notaDAO = JapeFactory.dao("CabecalhoNota");
					Collection<DynamicVO> notasVO = notaDAO.find("NUNOTA = ?", nuNota);
					for (@SuppressWarnings("unused") DynamicVO notaVO : notasVO) {				
						
						BigDecimal numeroNota = (BigDecimal) notaVO.getProperty("NUNOTA");
						BigDecimal numNota = (BigDecimal) notaVO.getProperty("NUMNOTA");
						
						System.out.println("gera��o do XML " + numeroNota + ".");
						
						if (xml.equals("S")) {
							//Inicio Gera��o do arquivo XML
							/*
						    byte[] arquivoCTe = null;

						    ServicosCTeHelper helper = new ServicosCTeHelper();

						    arquivoCTe = helper.gerarXMLDaNotaEmArquivo(numeroNota);
						    
						    arquivos.put(numNota + "CTEGLOBAL.xml",arquivoCTe);*/
							byte[] arquivoCTe = null;
							
							
							arquivoCTe = query.getString("ARQUIVOXML").getBytes();
							arquivos.put(numNota + "CTEGLOBAL.xml",arquivoCTe);
						    //Final Gera��o do arquivo XML							
						}
					    
					    if (nota.equals("S")) {
							//inicio gera��o do relat�rio fatura
							nroRelatorio = query.getBigDecimal("NURFE");
							parameters = new HashMap<String, Object>();
							
							ImpressaoNotaHelpper impressaoNotaHelpper = new ImpressaoNotaHelpper(); //getImagemQRCodeDanfeCTe;
													
							parameters.put("QRCODE",impressaoNotaHelpper.getImagemQRCodeDanfeCTe(numeroNota));
							parameters.put("NUNOTA", numeroNota);
							parameters.put("PDIR_MODELO", ArquivoModeloUtils.getDiretorioModelos());
										
							modeloImpressao = ReportManager.getInstance().getReport(nroRelatorio, dwfEntityFacade);
							
							jasperPrint = modeloImpressao.buildJasperPrint(parameters, jdbc.getConnection());
							
							arqFatura = JasperExportManager.exportReportToPdf(jasperPrint);

							arquivos.put(numNota + "CTEGLOBAL.pdf",arqFatura);			
							//final gera��o do relat�rio fatura					    	
					    }
											    
					}
				} else if (statusNFSe.equals("A") && codModDoc.equals("1")) {
					
					JapeWrapper notaDAO = JapeFactory.dao("CabecalhoNota");
					Collection<DynamicVO> notasVO = notaDAO.find("NUNOTA = ?", nuNota);
					for (@SuppressWarnings("unused")
					DynamicVO notaVO : notasVO) {
						
						BigDecimal numeroNota = (BigDecimal) notaVO.getProperty("NUNOTA");
						BigDecimal numNota = (BigDecimal) notaVO.getProperty("NUMNOTA");
						
						if (xml.equals("S")) {
							// Inicio Gera��o do arquivo XML
							byte[] arquivoNFSe = null;

							
							QueryExecutor queryNFSe = ctx.getQuery();
							
							queryNFSe.nativeSelect("SELECT XMLRPS FROM TGFNFSE WHERE NUNOTA = " + numeroNota + " ");
							
							if (queryNFSe.next()) {
								arquivoNFSe = queryNFSe.getString("XMLRPS").getBytes();
								arquivos.put(numNota + "NFSEGLOBAL.xml", arquivoNFSe);
							}
							
							// Final Gera��o do arquivo XML
						}
						
						if (nota.equals("S")) {

							
							
							// inicio gera��o do relat�rio fatura
							nroRelatorio = query.getBigDecimal("NURFE");
							parameters = new HashMap<String, Object>();

							@SuppressWarnings("unused")
							ImpressaoNotaHelpper impressaoNotaHelpper = new ImpressaoNotaHelpper(); // getImagemQRCodeDanfeCTe;

							parameters.put("NUNOTA", numeroNota);
							parameters.put("PDIR_MODELO", ArquivoModeloUtils.getDiretorioModelos());


							modeloImpressao = ReportManager.getInstance().getReport(nroRelatorio, dwfEntityFacade);

							jasperPrint = modeloImpressao.buildJasperPrint(parameters, jdbc.getConnection());

							arqFatura = JasperExportManager.exportReportToPdf(jasperPrint);

							arquivos.put(numNota + "NFSEGLOBAL.pdf",arqFatura);

							arqFatura = null;
							// final gera��o do relat�rio fatura
						}


					}
				}
			}
			
			new ZipUtils();
			@SuppressWarnings("unused")
			byte[] arquivo = ZipUtils.zip(arquivos);
			
			SessionFile fileReport = SessionFile.createSessionFile("Fatura" + numFatura + ".zip", SessionFile.MimeType.ZIP, arquivo);
			
			String chaveSessaoArquivo = UIDGenerator.getNextID();
			
			ServiceContext.getCurrent().putHttpSessionAttribute(chaveSessaoArquivo, (Serializable) fileReport);
		
			ctx.setMensagemRetorno(String.format("Arquivo gerado.\n %s", getLinkBaixar("Clique aqui para baixar.", chaveSessaoArquivo)));
						
		} catch (Exception e) {
			jdbc.closeSession();
			e.printStackTrace();
		} finally {
			jdbc.closeSession();
		}
	}

	private String getLinkBaixar(String descricao, String chave) {
		String url = "<a title=\"Visualizar Arquivo\" href=\"/mge/visualizadorArquivos.mge?chaveArquivo="+chave+"\" target=\"_blank\"><u><b>"+descricao+"</b></u></a>";

		return url;
	}
}
