package com.youssefhenna.policy.model;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

public record FileWithSignature(String baseName, @Nullable Path filePath, Path signatureFilePath) {

}
