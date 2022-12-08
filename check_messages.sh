#!/bin/bash

# Store the path to the directory containing the message files
messageDir="./conf/"

# Colour codes for pass/fail
GREEN="\032[0;31m"
RED="\033[0;31m"

# Check if the two message files exist
if [ -e "$messageDir/messages" ] && [ -e "$messageDir/messages.cy" ]; then

  # Store the keys from the messages files in arrays
  # A key must contain a full stop and must not contain // or #
  messageKeysEn=($(awk -F '=' '$1 ~ /[^#]*[.]/ && $1 !~ /[^#]*\/\// {print $1}' "$messageDir/messages"))
  messageKeysCy=($(awk -F '=' '$1 ~ /[^#]*[.]/ && $1 !~ /[^#]*\/\// {print $1}' "$messageDir/messages.cy"))

  # Iterate over the keys in the English messages file
  for key in "${messageKeysEn[@]}"; do
    # Check if the current key is present in the Welsh messages file
    if ! grep -q "$key" "$messageDir/messages.cy"; then
      # If the key is not present in the Welsh messages file, print it
      echo "Missing key in messages.cy: $key"
      # Set a flag to indicate that a missing key was found
      missing=true
    fi
  done

  # Iterate over the keys in the Welsh messages file
  for key in "${messageKeysCy[@]}"; do
    # Check if the current key is present in the English messages file
    if ! grep -q "$key" "$messageDir/messages"; then
      # If the key is not present in the English messages file, print it
      echo "Missing key in messages: $key"
      # Set a flag to indicate that a missing key was found
      missing=true
    fi
  done

  # Check if the missing flag is set
  if [ "$missing" = true ]; then
    # If the flag is set, exit with a status code of 1
    echo -e "${RED}Please correct the message keys and try again."
    exit 1
  else
    echo -e "${GREEN}All EN and CY message keys passed checks."
    exit 0
  fi

else
  # Print an error message if the message files do not exist
  printf "${RED}Error: Message files do not exist. Please check they exist in /conf/."
  exit 1
fi
