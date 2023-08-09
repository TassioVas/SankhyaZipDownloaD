package br.org.faepu.downloadAnexo;

import java.math.BigDecimal;
import java.sql.ResultSet;

import com.sankhya.util.SessionFile;
import com.sankhya.util.UIDGenerator;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class Program implements AcaoRotinaJava {
	
	BigDecimal nuNota;
	byte[] anexo;
	

	@Override
	public void doAction(ContextoAcao contexto) throws Exception {
		// TODO Auto-generated method stub
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		JdbcWrapper JDBC = JapeFactory.getEntityFacade().getJdbcWrapper();
		NativeSql nativeSql = new NativeSql(JDBC);
		SessionHandle hnd = null;
		Registro[] registros = contexto.getLinhas();
		Registro r = registros[0];
		// nome arquivo sessao
		//String chave = "text_" + UIDGenerator.getNextID();
		String chave = UIDGenerator.getNextID();
		//for (int i = 0; i < (contexto.getLinhas()).length; i++) {
		//	contexto.getLinhas()[i];
		
		  BigDecimal coddata = (BigDecimal) r.getCampo("CODATA");
	      String tipo = (String) r.getCampo("TIPO");
	      String descricao = (String)r.getCampo("DESCRICAO");
	      BigDecimal sequencia = (BigDecimal) r.getCampo("SEQUENCIA");
	      BigDecimal sequenciapr = (BigDecimal) r.getCampo("SEQUENCIAPR");
		
		
		nuNota = (BigDecimal) r.getCampo("NUNOTA");

			ResultSet rs = nativeSql.executeQuery("SELECT  ATA.CONTEUDO AS CONTEUDO , ATA.ARQUIVO AS ARQUIVO"
					+ " FROM "
					+ "	TGFCAB CAB "
					+ "	left JOIN TSIATA ATA ON ATA.CODATA = cab.NUNOTA "
					+ " WHERE NUNOTA  = " + nuNota);
			
			while (rs.next()) {
				anexo = rs.asBlob("ARQUIVO");
			}
			
			System.out.println("sysout " + rs);
			//System.out.println("sysout " + );
			

			try {
				//PersistentLocalEntity persistent = dwfEntityFacade.findEntityByPrimaryKey("Anexo", new Object[] { anexo });
			//	DynamicVO historicoVO = (DynamicVO)persistent.getValueObject();
				hnd = JapeSession.open();

				// byte array do arquivo que quer baixar
				byte[] fileContent = "teste ".getBytes();
				
				anexo = "teste ".getBytes();

				// instancia o arquivo zip
				SessionFile sessionFile = SessionFile.createSessionFile("zip.pdf", SessionFile.MimeType.PDF, anexo);

				// sobe o arquivo para a sessao do sankhya para a tela poder baixar
				ServiceContext.getCurrent().putHttpSessionAttribute(chave, sessionFile);

			} finally {
				JapeSession.close(hnd);
			}

			contexto.setMensagemRetorno("<a id=\"alink\" href=\"/mge/visualizadorArquivos.mge?chaveArquivo=" + chave
					+ "\" target=\"_blank\">Baixar: "/* + r.getCampo("NUNOTA")*/);
	
		
	}
}
