package com.group.moviediscoveryagent.chat;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class NdjsonEventWriter {
    private final ObjectMapper mapper = new ObjectMapper();

    public synchronized void writeEvent(OutputStream out, NdjsonEvent event) throws Exception {
        writeRawLine(out, toJson(event));
    }

    public String toJson(NdjsonEvent event) throws Exception {
        return mapper.writeValueAsString(event);
    }

    public synchronized void writeRawLine(OutputStream out, String jsonLine) throws Exception {
        // ensure one JSON object per line ending with LF
        out.write(jsonLine.getBytes(StandardCharsets.UTF_8));
        out.write('\n');
        out.flush();
    }
}


