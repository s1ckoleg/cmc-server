#!/bin/bash

# Script to migrate from com.example to com.cmc

# Create the new directory structure
mkdir -p src/main/kotlin/com/cmc
mkdir -p src/main/kotlin/com/cmc/auth
mkdir -p src/main/kotlin/com/cmc/database/services
mkdir -p src/main/kotlin/com/cmc/database/tables
mkdir -p src/main/kotlin/com/cmc/models
mkdir -p src/main/kotlin/com/cmc/models/auth
mkdir -p src/main/kotlin/com/cmc/routes
mkdir -p src/main/kotlin/com/cmc/services
mkdir -p src/main/kotlin/com/cmc/tasks
mkdir -p src/main/kotlin/com/cmc/utils

# Copy all files to the new structure
find src/main/kotlin/com/example -name "*.kt" | while read file; do
    # Get the relative path from com/example
    rel_path=${file#src/main/kotlin/com/example/}
    # Create the new path
    new_path="src/main/kotlin/com/cmc/$rel_path"
    # Copy the file
    cp "$file" "$new_path"
    echo "Copied $file to $new_path"
done

# Update package declarations and imports in all Kotlin files
find src/main/kotlin/com/cmc -name "*.kt" | while read file; do
    # Replace package declarations
    sed -i '' 's/package com\.example/package com.cmc/g' "$file"
    # Replace imports
    sed -i '' 's/import com\.example/import com.cmc/g' "$file"
    echo "Updated package references in $file"
done

# Update build.gradle.kts
sed -i '' 's/group = "com\.example"/group = "com.cmc"/g' build.gradle.kts
sed -i '' 's/mainClass\.set("com\.example\.ApplicationKt")/mainClass.set("com.cmc.ApplicationKt")/g' build.gradle.kts
echo "Updated build.gradle.kts"

echo "Migration completed. Please verify the changes and remove the old com.example directory if everything works correctly." 