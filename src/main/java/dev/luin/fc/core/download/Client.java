package dev.luin.fc.core.download;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;

import dev.luin.fc.core.file.FileSystem;
import dev.luin.fc.core.security.KeyStore;
import dev.luin.fc.core.security.KeyStoreType;
import dev.luin.fc.core.security.TrustStore;
import dev.luin.fc.core.upload.SSLFactoryManager;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.val;
import lombok.var;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class Client
{
	SSLSocketFactory sslSocketFactory;

	public static void main(String[] args) throws Exception
	{
		if (args.length == 0)
			System.out.println("Usage: Client <url>");
		val sslFactoryManager = SSLFactoryManager.builder()
				.keyStore(KeyStore.of(KeyStoreType.PKCS12,"dev/luin/fc/core/keystore.p12","password","password"))
				.trustStore(TrustStore.of(KeyStoreType.PKCS12,"dev/luin/fc/core/keystore.p12","password"))
				.enabledProtocols(new String[]{"TLSv1.2"})
				.enabledCipherSuites(new String[]{"TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384","TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"})
				.verifyHostnames(true)
				.build();
		val client = new Client(sslFactoryManager.getSslSocketFactory());
		client.download(args[0]);
	}

	private void download(String s) throws IOException
	{
		val url = new URL(s);
		var connection = createConnection(url);
		connection.setRequestMethod("HEAD");
		val contentLength = connection.getContentLengthLong();
		val chunkSize = 256;
		val file = FileSystem.getFile.apply(createRandomFile().get());
		long fileLength = 0;
		while (fileLength < contentLength)
		{
			fileLength += chunkSize;
			connection = createConnection(url);
		  connection.setRequestProperty("Range","bytes=" + file.length() + "-" + fileLength);
			try (val output = new FileOutputStream(file,true))
			{
				IOUtils.copyLarge(connection.getInputStream(),output);
			}
		}
		System.out.println(file.getAbsolutePath());
	}

	private java.net.HttpURLConnection createConnection(final java.net.URL url) throws IOException
	{
		val connection = (HttpURLConnection)url.openConnection();
		if (connection instanceof HttpsURLConnection)
		{
			HttpsURLConnection secureConnection = (HttpsURLConnection)connection;
			secureConnection.setSSLSocketFactory(sslSocketFactory);
	  }
		return connection;
	}

	private Try<String> createRandomFile()
	{
		var result = (Path)null;
		try
		{
			while (true)
			{
				val filename = RandomStringUtils.randomNumeric(32);
				result = Paths.get(filename);
				if (result.toFile().createNewFile())
					return Try.success(result.toString());
			}
		}
		catch (IOException e)
		{
			return Try.failure(new IOException("Error creating file " + result,e));
		}
	}
}
