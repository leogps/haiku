#!/usr/bin/env bash

echo "Cleaning up build-src files..."
# Check if arguments are provided
if [ $# -eq 0 ]; then
    echo "Nothing to delete"
    exit 0
fi

# Loop through each argument
for file in "$@"; do
    # Check if file exists
    if [ -e "$file" ]; then
        # Delete the file/folder
        rm -rf "$file"
        echo "Deleted: $file"
    else
        echo "File not found: $file"
    fi
done