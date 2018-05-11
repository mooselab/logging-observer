import java.io.FileNotFoundException;
import java.io.IOException;

public class CatchExceptionTest {

    public static void main(String[] args) {
        System.out.println("Start to test exception catching mechanism ...");
        try {
            exceptionThrowingMethod();
            throw new FileNotFoundException("Blabla.");
        } catch (FileNotFoundException e) {
            System.out.println("Caught FileNotFoundException.");
            System.out.println(e.toString());
            //throw new FileNotFoundException("File not found exception in catch block.");
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
