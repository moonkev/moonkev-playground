import javax.crypto.Cipher
import javax.crypto.EncryptedPrivateKeyInfo
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.CharBuffer
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.util
import java.util.Base64
import java.util.Optional
import java.util.regex.Pattern
import java.nio.charset.StandardCharsets.US_ASCII
import java.util.regex.Pattern.CASE_INSENSITIVE
import javax.crypto.Cipher.DECRYPT_MODE
import scala.collection.JavaConverters._


object PemReader {
  private val CERT_PATTERN = Pattern.compile(
    "-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+([a-z0-9+/=\\r\\n]+)-+END\\s+.*CERTIFICATE[^-]*-+",
    CASE_INSENSITIVE
  )

  private val KEY_PATTERN = Pattern.compile(
    "-+BEGIN\\s+.*PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+([a-z0-9+/=\\r\\n]+)-+END\\s+.*PRIVATE\\s+KEY[^-]*-+",
    CASE_INSENSITIVE
  )

  @throws[IOException]
  @throws[GeneralSecurityException]
  def loadTrustStore(certificateChainFile: File): KeyStore = {
    val keyStore = KeyStore.getInstance("JKS")
    keyStore.load(null, null)
    val certificateChain = readCertificateChain(certificateChainFile)

    for (certificate <- certificateChain.asScala) {
      val principal = certificate.getSubjectX500Principal
      keyStore.setCertificateEntry(principal.getName("RFC2253"), certificate)
    }
    keyStore
  }

  @throws[IOException]
  @throws[GeneralSecurityException]
  def loadKeyStore(certificateChainFile: File, privateKeyFile: File, keyPassword: Optional[String]): KeyStore = {
    val encodedKeySpec = readPrivateKey(privateKeyFile, keyPassword)
    val key =
      try {
        val keyFactory = KeyFactory.getInstance("RSA")
        keyFactory.generatePrivate(encodedKeySpec)
      } catch {
        case _: InvalidKeySpecException =>
          val keyFactory = KeyFactory.getInstance("DSA")
          keyFactory.generatePrivate(encodedKeySpec)
      }
    val certificateChain = readCertificateChain(certificateChainFile)
    if (certificateChain.isEmpty)
      throw new CertificateException("Certificate file does not contain any certificates: " + certificateChainFile)
    val keyStore = KeyStore.getInstance("JKS")
    keyStore.load(null, null)
    keyStore.setKeyEntry("key", key, keyPassword.orElse("").toCharArray, certificateChain.asScala.toArray)
    keyStore
  }

  @throws[IOException]
  @throws[GeneralSecurityException]
  private def readCertificateChain(certificateChainFile: File) = {
    val contents           = readFile(certificateChainFile)
    val matcher            = CERT_PATTERN.matcher(contents)
    val certificateFactory = CertificateFactory.getInstance("X.509")
    val certificates       = new util.ArrayList[X509Certificate]
    var start              = 0
    while (matcher.find(start)) {
      val buffer = base64Decode(matcher.group(1))
      certificates.add(
        certificateFactory.generateCertificate(new ByteArrayInputStream(buffer)).asInstanceOf[X509Certificate]
      )
      start = matcher.`end`
    }
    certificates
  }

  @throws[IOException]
  @throws[GeneralSecurityException]
  private def readPrivateKey(keyFile: File, keyPassword: Optional[String]): PKCS8EncodedKeySpec = {
    val content = readFile(keyFile)
    val matcher = KEY_PATTERN.matcher(content)
    if (!matcher.find)
      throw new KeyStoreException("found no private key: " + keyFile)
    val encodedKey = base64Decode(matcher.group(1))
    if (!keyPassword.isPresent) {
      new PKCS8EncodedKeySpec(encodedKey)
    } else {
      val encryptedPrivateKeyInfo = new EncryptedPrivateKeyInfo(encodedKey)
      val keyFactory              = SecretKeyFactory.getInstance(encryptedPrivateKeyInfo.getAlgName)
      val secretKey               = keyFactory.generateSecret(new PBEKeySpec(keyPassword.get.toCharArray))
      val cipher                  = Cipher.getInstance(encryptedPrivateKeyInfo.getAlgName)
      cipher.init(DECRYPT_MODE, secretKey, encryptedPrivateKeyInfo.getAlgParameters)
      encryptedPrivateKeyInfo.getKeySpec(cipher)
    }
  }

  private def base64Decode(base64: String) =
    Base64.getMimeDecoder.decode(base64.getBytes(US_ASCII))

  @throws[IOException]
  private def readFile(file: File) = {
    val reader = new InputStreamReader(new FileInputStream(file), US_ASCII)
    try {
      val stringBuilder = new StringBuilder
      val buffer        = CharBuffer.allocate(2048)
      while (reader.read(buffer) != -1) {
        buffer.flip
        stringBuilder.append(buffer)
        buffer.clear
      }
      stringBuilder.toString
    } finally if (reader != null) reader.close()
  }
}
