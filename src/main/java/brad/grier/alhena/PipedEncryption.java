package brad.grier.alhena;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedDataStreamGenerator;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class PipedEncryption {

    static {
        // This ensures BC is registered if it hasn't been already
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static class Encryptor {

        private final PipedInputStream inputPipe;
        private final PipedOutputStream outputPipe;
        private final ExecutorService executor;
        private final X509Certificate certificate;
        private Future<?> encryptionFuture;
        private final File outputFile;

        public Encryptor(X509Certificate certificate, File outputFile) throws IOException {
            this.certificate = certificate;
            this.outputFile = outputFile;
            this.inputPipe = new PipedInputStream(8192);  // Buffer size can be adjusted
            this.outputPipe = new PipedOutputStream(inputPipe);
            this.executor = Executors.newSingleThreadExecutor();
            startEncryption();
        }

        private void startEncryption() {
            encryptionFuture = executor.submit(() -> {
                try {
                    if (Security.getProvider("BC") == null) {
                        Security.addProvider(new BouncyCastleProvider());
                    }
                    // set up
                    CMSEnvelopedDataStreamGenerator edGen = new CMSEnvelopedDataStreamGenerator();
                    edGen.addRecipientInfoGenerator(
                            new JceKeyTransRecipientInfoGenerator(certificate)
                                    .setProvider("BC")
                    );
                
                    try (OutputStream fileOut = new FileOutputStream(outputFile); OutputStream encryptedOut = edGen.open(
                            fileOut,
                            new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES256_CBC)
                                    .setProvider("BC")
                                    .build()
                    )) {
                        // read from pipe and encrypt
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = inputPipe.read(buffer)) != -1) {
                            encryptedOut.write(buffer, 0, bytesRead);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        public void writeString(String data) throws IOException {
            outputPipe.write(data.getBytes(StandardCharsets.UTF_8));
            outputPipe.flush();
        }

        public void close() throws Exception {
            outputPipe.close();
            encryptionFuture.get();  // wait for encryption to complete
            executor.shutdown();
        }
    }
}
