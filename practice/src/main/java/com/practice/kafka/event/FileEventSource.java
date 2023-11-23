package com.practice.kafka.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutionException;

public class FileEventSource implements Runnable {

    public static final Logger logger = LoggerFactory.getLogger(FileEventSource.class.getName());


    boolean keepRunning = true;
    private int updateInterval;
    private File file;
    private long filePointer = 0;

    private EventHandler eventHandler;


    public FileEventSource(int updateInterval, File file, EventHandler eventHandler) {
        this.updateInterval = updateInterval;
        this.file = file;
        this.eventHandler = eventHandler;
    }

    @Override
    public void run() {

        try {
            while (this.keepRunning) {
                Thread.sleep(this.updateInterval);

                // file size 계산
                long len = this.file.length();
                if (len < this.filePointer) {
                    logger.info("file was reset as filePointer is longer than file length");
                    filePointer = len;
                } else if (len > this.filePointer) {
                    readAppendAndSend();
                } else {
                    continue;
                }

            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

    }

    private void readAppendAndSend() throws IOException, ExecutionException, InterruptedException {
        RandomAccessFile raf = new RandomAccessFile(this.file, "r");
        raf.seek(this.filePointer);

        String line = null;
        while ((line = raf.readLine()) != null) {
            sendMessage(line);
        }

        // file이 변경되었으므로 filePointer를 현재 file의 마지막으로 이동
        this.filePointer = raf.getFilePointer();


    }

    private void sendMessage(String line) throws ExecutionException, InterruptedException {
        String[] tokens = line.split(",");
        String key = tokens[0];
        StringBuffer value = new StringBuffer();

        for (int i = 1; i < tokens.length; i++) {
            if (i != tokens.length - 1) {
                value.append(tokens[i] + ",");
            } else {
                value.append(tokens[i]);
            }
        }

        MessageEvent messageEvent = new MessageEvent(key, value.toString());
        this.eventHandler.onMessage(messageEvent);

    }
}
