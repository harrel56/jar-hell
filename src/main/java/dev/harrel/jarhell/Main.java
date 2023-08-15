package dev.harrel.jarhell;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

public class Main {

    public static void main(String[] arg) throws IOException, InterruptedException, SAXException, ParserConfigurationException, XPathExpressionException {
        String group = "dev.harrel";
        String artifact = "json-schema";

        Processor processor = new Processor();
        processor.process(group, artifact);

    }
}
