package io.onellm.core;

import java.util.Objects;

/**
 * Represents a chat message with a role and content.
 */
public class Message {
    
    private final String role;
    private final String content;
    
    /**
     * Creates a new message.
     *
     * @param role    The role (system, user, assistant)
     * @param content The message content
     */
    public Message(String role, String content) {
        this.role = Objects.requireNonNull(role, "Role cannot be null");
        this.content = Objects.requireNonNull(content, "Content cannot be null");
    }
    
    /**
     * Creates a system message.
     */
    public static Message system(String content) {
        return new Message("system", content);
    }
    
    /**
     * Creates a user message.
     */
    public static Message user(String content) {
        return new Message("user", content);
    }
    
    /**
     * Creates an assistant message.
     */
    public static Message assistant(String content) {
        return new Message("assistant", content);
    }
    
    public String getRole() {
        return role;
    }
    
    public String getContent() {
        return content;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return role.equals(message.role) && content.equals(message.content);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(role, content);
    }
    
    @Override
    public String toString() {
        return "Message{role='" + role + "', content='" + 
               (content.length() > 50 ? content.substring(0, 50) + "..." : content) + "'}";
    }
}
