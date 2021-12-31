import ithakimodem.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Requests
{
    Modem m;
    String path;
    int receivedByte;

    public Requests()
    {
        m = new Modem();
        m.setSpeed(80000);
        m.setTimeout(2000);
        m.open("ithaki");
        path = "";
    }

    public static void main(String[] args) throws IOException
    {
        Requests r = new Requests();
        String code;

        r.helloStrangerMessage();

        System.out.print("Give me a path to store your files : ");
        r.path = (new Scanner(System.in)).nextLine();

        for (int request=0; request<5; request++) {
            System.out.print("Give me a code : ");
            code = (new Scanner(System.in)).nextLine();
            r.pickFunctionBasedOnRequest(code);
        }

        r.m.close();
    }

    public void helloStrangerMessage()
    {
        String hello = "";
        while (!hello.contains("\r\n\n\n")) {
            receivedByte = m.read();
            if (receivedByte == -1)
                break;
            hello += (char) receivedByte;
        }
    }

    public void pickFunctionBasedOnRequest(String code) throws IOException
    {
        switch (code.charAt(0)) {
            case 'E' -> getEcho(code + "\r");
            case 'M' -> getImage(code + "\r", false);
            case 'G' -> getImage(code + "\r", true);
            case 'P' -> {
                System.out.print("Give me the R parameter : ");
                String R = (new Scanner(System.in)).nextLine();
                getGPS(code, R + "\r");
            }
            case 'Q' -> {
                System.out.print("Give me the NACK code : ");
                String nack = (new Scanner(System.in)).nextLine();
                getArq(code + "\r", nack + "\r");
            }
        }
    }

    public void getEcho(String code) throws IOException
    {
        /* ------------------- Variables ------------------ */
        String packet;
        long startOfPacket, endOfPacket = 0, elapsedTime;
        long startOfMeasurement;
        long sixMinutes = 6L * 60 * 1000 * 1000 * 1000;
        /* ------------------------------------------------ */

        /* ------------------ File Creation ------------------ */
        String responsesPath = path + "\\echo_responses.csv";
        FileWriter responses = new FileWriter(responsesPath, true);
        PrintWriter print_response = new PrintWriter(responses);
        /* --------------------------------------------------- */

        startOfMeasurement = System.nanoTime();

        /* ------------------------ Echopackets ------------------------ */
        while ((System.nanoTime() - startOfMeasurement) < sixMinutes) {
            packet = ""; // always reset the string to get new packet
            m.write(code.getBytes());
            startOfPacket = System.nanoTime(); // time starts after write()
            while (!packet.contains("PSTOP")) {
                receivedByte = m.read();
                if (receivedByte == -1)
                    break;
                packet += (char) receivedByte;
                endOfPacket = System.nanoTime();
            }
            elapsedTime = (endOfPacket - startOfPacket) / 1_000_000; //ns -> ms
            print_response.printf(Long.toString(elapsedTime) + "\n");
        }
        print_response.close();
        /* ------------------------------------------------------------ */
    }

    public void getImage(String code, boolean imageHasError) throws IOException
    {
        /* ----------------- Variables ---------------- */
        String imageOfChars = "";
        String FF = "ÿ", D8 = "Ø", D9 = "Ù";
        /* -------------------------------------------- */

        /* -------------------- File Creation -------------------- */
        String imagePath;
        if (imageHasError) {
            imagePath = path + "\\imageErrorYes.jpg";
        }
        else {
            imagePath = path + "\\imageErrorNo.jpg";
        }
        FileOutputStream image = new FileOutputStream(imagePath);
        /* ------------------------------------------------------- */

        image.write(255); // 0xFF start delimiter
        image.write(216); // 0xD8 start delimiter

        m.write(code.getBytes());
        while (!imageOfChars.contains(FF+D9)) {
            receivedByte = m.read();
            if (receivedByte == -1)
                break;
            imageOfChars += (char) receivedByte;
            if (imageOfChars.contains(FF+D8))
                image.write(receivedByte);
        }
        /* ------------------------------------------------------------ */
        image.close();
    }

    public void getGPS(String code, String R) throws IOException
    {
        /* ------------- Variables ------------- */
        String trace = "", selectedTraces = "";
        int currentLine = 0;
        String charImage = "";
        String FF = "ÿ", D8 = "Ø", D9 = "Ù";
        /* ------------------------------------- */

        /* ------------------------- File Creation -------------------------- */
        FileOutputStream gps = new FileOutputStream(path + "\\gps.jpg");
        /* ------------------------------------------------------------------ */

        /* ------------------ Picking Points ------------------ */
        /*
        * From all the traces I get, I choose only those that are
        *                      10 lines apart
        */
        m.write((code + R).getBytes());
        while (!selectedTraces.contains("STOP ITHAKI GPS TRACKING")) {
            while (!trace.contains("\r\n")) {
                receivedByte = m.read();
                if (receivedByte == -1)
                    break;
                trace += (char) receivedByte;
            }
            if (currentLine%10 == 0)
                selectedTraces += trace;
            currentLine++;
            trace = "";
        }
        /*
         * Having the selected traces I split them firstly by trace (using the
         *      "\r\n") and then in each trace, using the "," I pick
         *      4th element -> east coordinate
         *      2nd element -> north coordinate
         */
        ArrayList<Double> llcoord = new ArrayList<>();

        for (int i=1; i<selectedTraces.split("\r\n").length-1; i++) {
            llcoord.add(Double.parseDouble(selectedTraces.split("\r\n")[i].split(",")[4]));
            llcoord.add(Double.parseDouble(selectedTraces.split("\r\n")[i].split(",")[2]));
        }
        /*
         * Now after a conversion I format all the traces and add them to the
         *                      final T=... parameter
         */
        String tParameters = "";

        for (int i=0; i<llcoord.size(); i++) {
            if (i%2 == 0)
                tParameters += "T="
                        + String.valueOf(llcoord.get(i)).split("\\.")[0]
                        + String.valueOf(Integer.parseInt(String.valueOf(llcoord.get(i)).split("\\.")[1]) * 0.6).substring(0, 2);
            if (i%2 != 0)
                tParameters += String.valueOf(llcoord.get(i)).split("\\.")[0]
                        + String.valueOf(Integer.parseInt(String.valueOf(llcoord.get(i)).split("\\.")[1]) * 0.6).substring(0, 2);
        }

        gps.write(255); // 0xFF start
        gps.write(216); // 0xD8 start

        m.write((code + tParameters + "\r").getBytes());

        while (!charImage.contains(FF+D9)) {
            receivedByte = m.read();
            if (receivedByte == -1)
                break;
            charImage += (char) receivedByte;
            if (charImage.contains(FF+D8))
                gps.write(receivedByte);
        }
        gps.close();
    }

    public void getArq(String ackCode, String nackCode) throws IOException
    {
        /* ------------------- Variables ------------------ */
        int ack = 0, nack = 0, counter;
        String packet;
        long startOfPacket, endOfPacket, elapsedTime;
        long startOfMeasurement;
        long sixMinutes = (long) 60 * 1000 * 1000 * 1000;
        /* ------------------------------------------------ */

        /* ------------------ File Creation ------------------ */
        FileWriter responses = new FileWriter(path + "\\arq_responses.csv", true);
        PrintWriter print_response = new PrintWriter(responses);
        FileWriter M = new FileWriter(path + "\\arq_M.csv", true);
        PrintWriter print_M = new PrintWriter(M);
        /* --------------------------------------------------- */

        /* --------------------- ACK-NACK Packets --------------------- */
        startOfMeasurement = System.nanoTime();
        while ((System.nanoTime() - startOfMeasurement) < sixMinutes) {
            packet = "";
            /* ------------- ACK ------------- */
            m.write(ackCode.getBytes());
            startOfPacket = System.nanoTime();
            while (!packet.contains("PSTOP")) {
                receivedByte = m.read();
                if (receivedByte == -1)
                    break;
                packet += (char) receivedByte;
            }
            ack++;
            /* ------------------------------- */

            endOfPacket = System.nanoTime(); // in case it is the correct one
            counter = 0;

            /* -------------- NACK -------------- */
            while (CryptoAndFCSArentEqual(packet)) {
                packet = "";
                counter++;
                m.write(nackCode.getBytes());
                while (!packet.contains("PSTOP")) {
                    receivedByte = m.read();
                    if (receivedByte == -1)
                        break;
                    packet += (char) receivedByte;
                    endOfPacket = System.nanoTime();
                }
            }
            nack += counter;
            /* ---------------------------------- */

            elapsedTime = (endOfPacket - startOfPacket) / 1_000_000;

            print_response.printf(Long.toString(elapsedTime) + "\n");
            print_M.printf(Integer.toString(counter) + "\n");
        }
        print_response.close();
        responses.close();

        print_M.close();
        M.close();
        /* ----------------------------------------------------------- */

        /* -------------------------- Calculation of BER -------------------------- */
        System.out.println(1 - Math.pow((double) ack/(ack + nack), (double) 1/128));
        /* ------------------------------------------------------------------------ */
    }

    public static boolean CryptoAndFCSArentEqual(String packet)
    {
        /* ------------- Create the FCS variable ------------- */
        int fcs = Integer.parseInt(packet.split(" ")[5]);
        /* ----------------------------------------------------- */

        /* -------------------- Calculation of XOR -------------------- */
        ArrayList<Integer> crypto = new ArrayList<>();

        for (int i=packet.indexOf("<")+1; i<packet.indexOf(">"); i++)
            crypto.add((int)(packet.charAt(i)));

        while (crypto.size() > 1) {
            crypto.set(0, crypto.get(0) ^ crypto.get(1));
            crypto.remove(1);
        }
        /* ------------------------------------------------------------ */

        /* ----- Return Statement ----- */
        return fcs != crypto.get(0);
        /* ---------------------------- */
    }
}