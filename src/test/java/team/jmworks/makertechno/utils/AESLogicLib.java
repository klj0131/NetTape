package team.jmworks.makertechno.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**This lib is a simple reference for these methods, let them clearer.
 * @author MakerTechno
 */
@SuppressWarnings("unused")
public class AESLogicLib {
/*------------------------------------------------FOR KEY GENERATIONS-------------------------------------------------*/
    /**Default String for class.*/
    private static final String ALGORITHM = "AES";

    /**Special enum to avoid entering the wrong AES format.*/
    public enum KeyLength{
        /**Use {@link KeyLength#getNum()} to return its number.*/
        AES_128,
        /**Use {@link KeyLength#getNum()} to return its number.*/
        AES_192,
        /**Use {@link KeyLength#getNum()} to return its number.*/
        AES_256;

        /**Here we have a method to return the right number.
         * @return An int */
        public int getNum(){
            if (this.equals(AES_128)) return 128;
            else if (this.equals(AES_192)) return 192;
            else return 256;
        }

        @Override
        public String toString() {
            if (this.equals(AES_128))return "128位AES(推荐)";
            if (this.equals(AES_192))return "192位AES";
            else return "256位AES";
        }
    }

    public static class KeyFormer{
        private final SecretKeySpec keySpec;
        /**Generate secret key, it supports the encrypt and decrypt progress
         * @param password The password which must be entered.
         * @param salt The salt byte array for generations.
         * @param iterations The iterations of the key. Suggest 10000 for best, it may not be too large(over 32768).
         * @param length The length for encode AES itself. Suggest {@link KeyLength#AES_128}
         */
        public KeyFormer(
                @NotNull String password, Salts salt, int iterations, @NotNull KeyLength length, Logger logger
        ) throws NoSuchAlgorithmException, InvalidKeySpecException, IteInputException {
            keySpec = generateAESKey(password, salt, iterations, length, logger);
        }
        /**Generate secret key, it supports the encrypt and decrypt progress
         * @param password The password which must be entered.
         * @param iterations The iterations of the key. Suggest 10000 for best, it may not be too large(over 32768).
         * @param length The length for encode AES itself. Suggest {@link KeyLength#AES_128}
         */
        public KeyFormer(
                @NotNull String password, int iterations, KeyLength length, Logger logger
        ) throws IteInputException, NoSuchAlgorithmException, InvalidKeySpecException{
            keySpec = generateAESKey(password, iterations, length, logger);
        }
        /**Generate secret key, it supports the encrypt and decrypt progress
         * @param password The password which must be entered.
         * @param salt The salt byte array for generations.
         * @param length The length for encode AES itself. Suggest {@link KeyLength#AES_128}
         */
        public KeyFormer(
                @NotNull String password, Salts salt, KeyLength length, Logger logger
        ) throws IteInputException, NoSuchAlgorithmException, InvalidKeySpecException{
            keySpec = generateAESKey(password, salt, length, logger);
        }
        /**Generate secret key, it supports the encrypt and decrypt progress
         * @param password The password which must be entered.
         * @param length The length for encode AES itself. Suggest {@link KeyLength#AES_128}
         */
        public KeyFormer(
                @NotNull String password, KeyLength length, Logger logger
        ) throws IteInputException, NoSuchAlgorithmException, InvalidKeySpecException{
            keySpec = generateAESKey(password, length, logger);
        }
        /**Generate secret key, it supports the encrypt and decrypt progress
         * @param password The password which must be entered.
         */
        public KeyFormer(
                @NotNull String password, Logger logger
        ) throws IteInputException, NoSuchAlgorithmException, InvalidKeySpecException {
            keySpec = generateAESKey(password, logger);
        }
    }



    @Contract("_, _, _, _, _ -> new")
    private static @NotNull SecretKeySpec generateAESKey(
            @NotNull String password, @NotNull Salts saltLength, int iterations, @NotNull KeyLength length, Logger logger
    ) throws NoSuchAlgorithmException, InvalidKeySpecException, IteInputException {
        checkIte(iterations, logger);
        byte[] salt = new byte[saltLength.getSalt()];
        //Get key generator by creating instance.
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, length.getNum());
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = secretKeyFactory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
    private static @NotNull SecretKeySpec generateAESKey(
            @NotNull String password, int iterations, KeyLength length, Logger logger
    ) throws IteInputException, NoSuchAlgorithmException, InvalidKeySpecException {
        return generateAESKey(password, Salts.A, iterations, length, logger);
    }
    private static @NotNull SecretKeySpec generateAESKey(
            @NotNull String password, Salts salt, KeyLength length, Logger logger
    ) throws IteInputException, NoSuchAlgorithmException, InvalidKeySpecException {
        return generateAESKey(password, salt, 10000, length, logger);
    }
    private static @NotNull SecretKeySpec generateAESKey(
            @NotNull String password, KeyLength length, Logger logger
    ) throws IteInputException, NoSuchAlgorithmException, InvalidKeySpecException {
        return generateAESKey(password, 10000, length, logger);
    }
    private static @NotNull SecretKeySpec generateAESKey(
            @NotNull String password, Logger logger
    ) throws IteInputException, NoSuchAlgorithmException, InvalidKeySpecException {
        return generateAESKey(password, KeyLength.AES_128, logger);
    }

    /**Special check for the iterations to make sure it will not throw any other errors.*/
    private static void checkIte(int iteLength, Logger logger) throws IteInputException {
        if (iteLength <= 0) throw new IteInputException(IteInputException.LESS_THAN_0);
        if (iteLength >32768) logger.warning("Warning: iteration value was set over 32768");//警告超出32768
    }



/*---------------------------------------------FOR ITEM ENCRYPT/DECRYPT----------------------------------------------*/
    /**Main calculate method.
     * @param inputFile The file will be encrypted/decrypted.
     * @param outputFile The finish output path.
     * @param former The password which the user inputted, see {@link KeyFormer}.
     * @param mode Encrypt or decrypt, Please use {@link Cipher#ENCRYPT_MODE}/{@link Cipher#DECRYPT_MODE} only.
     * @param withFileName True if you need to remember it. But be careful if the file was not doing the same before.
     * @return A string with fileName,
     * null if run with "no filename" or with encrypt mode,
     * "error" if the progress failed or not the expected mode.*/
    public static String encryptOrDecryptFile(
            File inputFile, File outputFile, @NotNull KeyFormer former, int mode, boolean withFileName, Logger logger
    ) throws EncryptOrDecryptException{
        try {
            SecretKeySpec secretKey = former.keySpec;
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(mode, secretKey);

            try (FileInputStream inputStream = new FileInputStream(inputFile);
                 OutputStream outputStream = new FileOutputStream(outputFile);
                 CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipher);
                 CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher)) {
                if (mode == Cipher.ENCRYPT_MODE) {
                     return extraFileFunc(inputFile, inputStream, cipherOutputStream, withFileName, true);
                } else  if (mode == Cipher.DECRYPT_MODE){
                    return extraFileFunc(inputFile, cipherInputStream, outputStream, withFileName, false);
                } else {
                    throw new EncryptOrDecryptException("You are using unexpected mode other than encrypt/decrypt, we can't finish it.");
                }
            }
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IOException e) {
            logger.log(Level.SEVERE, "An exception was thrown during encrypt/decrypt:", e);
            throw new EncryptOrDecryptException("Progress failed as: "+e.getMessage());
        }
    }


    /**Simply encrypt the file and output to the output path.*/
    public static void encryptFile(File inputFile, File outputFile, KeyFormer former, Logger logger) throws EncryptOrDecryptException {
        encryptOrDecryptFile(inputFile, outputFile, former,Cipher.ENCRYPT_MODE, false, logger);
    }
    /**Encrypt the file WITH ITS FILE NAME and output to the output path.
     IT MUST DECRYPT WITH FILENAME.*/
    public static void encryptFileWithName(@NotNull File inputFile, File outputFile, KeyFormer former, Logger logger) throws EncryptOrDecryptException {
        encryptOrDecryptFile(inputFile, outputFile, former,Cipher.ENCRYPT_MODE, true, logger);
    }
    /**Simply decrypt the file and output to the output path.*/
    public static void decryptFile(File inputFile, File outputFile, KeyFormer former, Logger logger) throws EncryptOrDecryptException {
        encryptOrDecryptFile(inputFile, outputFile, former, Cipher.DECRYPT_MODE, false, logger);
    }
    /**Decrypt the file WITH ITS ENCRYPTED FILE NAME and output to the output path.
     IT MUST BE ENCRYPTED WITH FILENAME.*/
    public static String decryptFileWithName(@NotNull File inputFile, File outputFile, KeyFormer former, Logger logger) throws EncryptOrDecryptException {
        return encryptOrDecryptFile(inputFile, outputFile, former,Cipher.DECRYPT_MODE, true, logger);
    }


    /**Extra controllable method for different output style.*/
    public static <I extends InputStream,O extends OutputStream> String extraFileFunc(
            File inputFile, I inputStream, O outputStream, boolean withFilename, boolean isEncrypt
    ) throws IOException {
        String fileName;
        if (withFilename) {
            if (isEncrypt) {
                //Add filename and put to stream first.
                fileName = inputFile.getName();
                byte[] fileNameBytes = fileName.getBytes(StandardCharsets.UTF_8);
                outputStream.write(fileNameBytes.length);
                outputStream.write(fileNameBytes);
            } else {
                //Get the filename first.
                int fileNameLength = inputStream.read();
                byte[] fileNameBytes = new byte[fileNameLength];
                if (inputStream.read(fileNameBytes) == -1) throw new EOFException("Are you kidding me? there's nothing to read!");
                fileName = new String(fileNameBytes, StandardCharsets.UTF_8);
            }
        } else {
            fileName = null;
        }
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return fileName;
    }
    /**
     * 盐值固定，虽然本来是可以随便选的...
     */
    public enum Salts {
        A, B, C, D;

        @Override
        public String toString() {
            return switch (this) {
                case A -> "16(默认)";
                case B -> "24";
                case C -> "48";
                case D -> "64";
            };
        }

        public int getSalt() {
            return switch (this) {
                case A -> 16;
                case B -> 24;
                case C -> 48;
                case D -> 64;
            };
        }
    }
    /*Test example
    public static void main(String[] args) {
        String inputFile = "path/to/input/file";
        String encryptedFile = "path/to/encrypted/file";
        String decryptedFile = "path/to/decrypted/file";
        String password = "your_password";


        encryptFile(new File(inputFile), new File(encryptedFile), password);
        decryptFile(new File(encryptedFile), new File(decryptedFile), password);
    }*/

    /**This exception throws when a salt value created by Locker failed by the following reasons*/
    public static class IteInputException extends Exception{
        public static final String LESS_THAN_0 = "Iteration value must be over than 0.";

        public IteInputException(String message){
            super(message);
        }
    }
    /**This exception throws when an encrypt/decrypt process failed.*/
    public static class EncryptOrDecryptException extends Exception{

        public EncryptOrDecryptException(String message){
            super(message);
        }
    }
}