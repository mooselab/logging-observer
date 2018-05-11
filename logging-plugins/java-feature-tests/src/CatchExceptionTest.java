import java.io.FileNotFoundException;
import java.io.IOException;

public class CatchExceptionTest {

    public static void main(String[] args) {
        System.out.println("Start to test exception catching mechanism ...");
        try {
            exceptionThrowingMethod();
        } catch (FileNotFoundException e) {
            System.out.println("Caught FileNotFoundException.");
            System.out.println(e.toString());
        } catch (IOException e) {
            System.out.println("Caught IOException");
            System.out.println(e.toString());
        }
    }

    public static void exceptionThrowingMethod() throws IOException {
        //throw new IOException("File Not Found!");
        throw new FileNotFoundException("File Not Found!");
    }

}
