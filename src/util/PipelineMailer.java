package util;

/**
 * Small utility class for sending emails easily
 * @author brendan
 *
 */
public class PipelineMailer {

	public static boolean sendMail(String recipient, String subject, String text) {
		return SendMail.sendEMail(recipient, from, subject, text, username, passwd);
	}
	
	private static final String from = "pipelinedaemon@fastmail.fm";
	private static final String username = "brendanofallon@fastmail.fm";
	private static final String passwd = "ILMdog64";
	
}
