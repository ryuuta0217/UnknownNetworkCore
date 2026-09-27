package net.unknown.provider.util;

import javax.annotation.Nonnull;
import java.util.Objects;

public record NamespacedKey(@Nonnull String namespace, @Nonnull String path) {
    private static final String DEFAULT_NAMESPACE = "unknown-network";

    public NamespacedKey {
        if (!isValidNamespace(namespace)) {
            throw new IllegalArgumentException("Invalid namespace: " + namespace);
        }

        if (!isValidPath(path)) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
    }

    public static NamespacedKey tryParse(String input) throws IllegalArgumentException {
        if (input == null || input.isEmpty()) return null;
        String[] parts = input.split(":", 2);
        if (parts.length != 2 && isValidPath(input)) {
            return new NamespacedKey(DEFAULT_NAMESPACE, input);
        } else {
            String namespace = parts[0];
            String path = parts[1];
            return new NamespacedKey(namespace, path);
        }
    }

    public static boolean isValidNamespace(String namespace) {
        if (namespace == null || namespace.isEmpty()) return false;
        return namespace.matches("[a-z0-9_\\-.]+");

    }

    public static boolean isValidPath(String path) {
        if (path == null || path.isEmpty()) return false;
        return path.matches("[a-z0-9_\\-./]+");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        NamespacedKey that = (NamespacedKey) obj;
        return Objects.equals(namespace, that.namespace) && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, path);
    }
}
