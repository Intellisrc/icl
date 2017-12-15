#!/bin/bash
echo -e "\e[93m STARTING COMPARISON ********************************************"
echo -e "\e[39m"
compare_with="../../groovy/common-java"
for F in $(find -name *.groovy); do
	base=$(basename $F);
	with=$(find $compare_with -name "$base")
	if [[ $with != "" ]]; then
		diffwith=$(diff $F $with | egrep -v "sharelock|inspeedia" | egrep "^[>|<]");
		if [[ $diffwith != "" ]]; then
			echo -e "\e[39m"
			echo -e "\e[36m${F} ..."
			echo -e "\e[39m"
			output=$(echo "$diffwith" | sed 's/^</\\e[92m</' | sed 's/^>/\\e[91m>/')
			echo -e "$output"
			echo -e "\e[39m"
		fi
	fi
done
echo -e "\e[39m"
