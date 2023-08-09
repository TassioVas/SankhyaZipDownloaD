package br.org.faepu.downloadAnexo;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.sankhya.util.SessionFile;
import com.sankhya.util.UIDGenerator;
import com.sankhya.util.ZipUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class AnexoArquivo implements AcaoRotinaJava {

	BigDecimal nuNota;
	JdbcWrapper JDBC = JapeFactory.getEntityFacade().getJdbcWrapper();
	NativeSql nativeSql = new NativeSql(JDBC);

	BigDecimal coddata;
	String tipo;
	String descricao;
	BigDecimal sequencia;
	BigDecimal sequenciapr;

	Map<String, byte[]> arquivos = new HashMap<String, byte[]>();
	
	/*Esse codigo Zipa os anexos da TSIATA porem nao cocatena, eles ficam separados dentro de um arquivo zipado*/

	public void doAction(ContextoAcao ctx) throws Exception {

		System.out.println("sysout Inicio o codigo");

		for (int i = 0; i < ctx.getLinhas().length; i++) {
			Registro line = ctx.getLinhas()[i];

			System.out.println("AbrirAnexo Inicio");

			try {
				nuNota = (BigDecimal) line.getCampo("NUNOTA");
				if (nuNota == null) {
					ctx.setMensagemRetorno("Documento Gerado no financeiro!");
					return;
				}
				System.out.println("Sysout nunota : " + nuNota);
				gerarRelatorio(ctx, line, nuNota);

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	private void gerarRelatorio(ContextoAcao ctx, Registro line, BigDecimal nuNota) throws Exception {

		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();

		ConcatenatePDF concatenate = new ConcatenatePDF();

		try {

			jdbc.openSession();
			ResultSet rs = nativeSql.executeQuery("SELECT  CONTEUDO, "
					+ " CODATA, TIPO, DESCRICAO, SEQUENCIA, SEQUENCIAPR " + "	FROM " + "	TGFCAB CAB "
					+ "		left JOIN TSIATA ATA ON ATA.CODATA = cab.NUNOTA " + "		WHERE NUNOTA  =" + nuNota);

			System.out.println("Sysout " + nuNota);

			while (rs.next()) {
				coddata = (BigDecimal) rs.getBigDecimal("CODATA");
				tipo = (String) rs.getString("TIPO");
				descricao = (String) rs.getString("DESCRICAO");
				sequencia = (BigDecimal) rs.getBigDecimal("SEQUENCIA");
				sequenciapr = (BigDecimal) rs.getBigDecimal("SEQUENCIAPR");

				if (coddata == null) {
					ctx.setMensagemRetorno("Anexo indisponivel para esse documento!");
					return;
				}

				System.out.println("SYSOUT " + coddata);
				System.out.println("SYSOUT " + tipo);
				System.out.println("SYSOUT " + descricao);
				System.out.println("SYSOUT " + sequencia);
				System.out.println("SYSOUT " + sequenciapr);

				PersistentLocalEntity persistent = dwfEntityFacade.findEntityByPrimaryKey("Anexo",
						new Object[] { coddata, tipo, descricao, sequencia, sequenciapr });
				DynamicVO historicoVO = (DynamicVO) persistent.getValueObject();
				byte[] conteudo = historicoVO.asBlob("CONTEUDO");

				//String nomeArquivo = historicoVO.asString("DESCRICAO");
				
				//System.out.println("SYSOUT " + nomeArquivo);

				ByteArrayOutputStream bytes = concatenate.run();

				//arquivos.put(nomeArquivo + "Anexo.pdf", conteudo);
			}
			
			new ZipUtils();
			@SuppressWarnings("unused")
			byte[] arquivo = ZipUtils.zip(arquivos);

			SessionFile fileReport = SessionFile.createSessionFile(nuNota + ".zip",SessionFile.MimeType.ZIP, arquivo);

			String chaveSessaoArquivo = UIDGenerator.getNextID();
			ServiceContext.getCurrent().putHttpSessionAttribute(chaveSessaoArquivo, (Serializable) fileReport);
			ctx.setMensagemRetorno(String.format("Arquivo ZIP dos Anexos gerado" + "\n %s",
					getLinkBaixar("Clique aqui para baixar.", chaveSessaoArquivo)));

		} catch (Exception e) {
			jdbc.closeSession();
			e.printStackTrace();
		} finally {
			jdbc.closeSession();
		}

	}

	private String getLinkBaixar(String descricao, String chave) {
		String url = "<a title=\"Visualizar Arquivo\" href=\"/mge/visualizadorArquivos.mge?chaveArquivo=" + chave
				+ "\" target=\"_blank\"><u><b>" + descricao + "</b></u></a>";

		return url;
	}

	private String getLinkBaixarPDF(String descricao, String chave) {

		String script = "Anexo encontrado." + "<script> " + "(function () { "
				+ "const link = document.createElement( 'a' ); "
				+ "link.href = '/mge/visualizadorArquivos.mge?chaveArquivo=" + chave + "'; "
				+ "link.target = '_blank'; " + "document.body.appendChild (link); " + "link.click (); "
				+ "document.body.removeChild (link); " + "}) (); " + "</script> ";

		return script;
	}

}
