package com.surakshafin.literacy;

public class Dtos {
    public record ContentView(Long id, String title, String body, String topic, String language, String format) {
        public static ContentView from(LiteracyContent c) {
            return new ContentView(c.getId(), c.getTitle(), c.getBody(), c.getTopic(), c.getLanguage(), c.getFormat());
        }
    }
}
