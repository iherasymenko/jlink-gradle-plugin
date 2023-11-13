package com.github.iherasymenko.jlink.test.fixtures;

public final class Text {

    public static String[] linesBetweenTags(String text, String startTag, String endTag) {
        int start = text.indexOf(startTag);
        if (start == -1) {
            throw new IllegalArgumentException("Start tag not found");
        }
        int end = text.indexOf(endTag, start + startTag.length());
        if (end == -1) {
            throw new IllegalArgumentException("End tag not found");
        }
        return text.substring(start + startTag.length(), end).trim().split("\\R");
    }

}
