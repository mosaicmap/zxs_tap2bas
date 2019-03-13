#!/bin/bash

out_dir="."
out_file_ext="bas_"
tap_dir="."

# "tmp/file.txt" --> "file"
# použití: $(plain_file_name $file)
function plain_file_name() {
  tmp_file_name=$(basename "$1")
  echo "${tmp_file_name%.*}"
}

cd "$(dirname $0)"
script_dir="$(pwd)"
cd - > /dev/null

for i in $tap_dir/*.tap; do
  base_name=$(plain_file_name "$i")
  out="$out_dir/$base_name.$out_file_ext"
  echo " * $i  ->  $out"
  java -Djava.util.logging.config.file="$script_dir/logging.properties" -jar "$script_dir/zxs_tap2bas.jar" -i "$i" -o "$out"
done
