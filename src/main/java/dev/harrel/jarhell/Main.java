package dev.harrel.jarhell;

import io.javalin.Javalin;

import javax.xml.parsers.ParserConfigurationException;

public class Main {

    public static void main(String[] arg) throws ParserConfigurationException {
        Processor processor = new Processor();

        Javalin.create()
                .get("/analyze", new AnalyzeHandler(processor))
                .start(8060);
    }
}
