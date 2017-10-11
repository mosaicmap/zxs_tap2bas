#!/bin/bash

# "file.tap" --> "file"
# použití: $(cutExt $1)
function cutExt()
{
  bExt=${1##*\.}
  echo "$(basename $1 .$bExt)"
}


###########################################

cd `dirname $0`
script_dir=`pwd`
cd - > /dev/null

for i in *.tap; do
    out="$(cutExt $i).bas"
    echo " * $i -> $out"
    java -Djava.util.logging.config.file="$script_dir/logging.properties" -jar "$script_dir/zxs_tap2bas.jar" -i $i --onlyBasic -o $out
    #java -Djava.util.logging.config.file="$script_dir/logging.properties" -jar "$script_dir/zxs_tap2bas.jar" -i $i -o $out
done

