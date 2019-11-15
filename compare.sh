#!/bin/bash
echo -e "\e[93m STARTING COMPARISON ********************************************"
echo -e "\e[39m"
compare_with="../../groovy/common-java"
for M in modules/*; do
	M=$(basename $M)
	echo "*********** $M ***********"
	for F in $(find modules/$M/ -name "*.groovy"); do
		base=$(basename $F);
		with=$(find ${compare_with}/modules/$M/ -name "$base" | grep -v "groovydoc")
		if [[ $with != "" ]]; then
			diffwith=$(diff $F $with | egrep -v "intellisrc|inspeedia" | egrep "^[>|<]");
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
done
for F in $(find modules/ -name "*.gradle"); do
	base=$(basename $F);
	fpath=$(dirname $F);
	with=$(find ${compare_with}/${fpath} -name "$base")
	if [[ $with != "" ]]; then
		diffwith=$(diff $F $with);
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
echo ""
echo "----- Looking for new files: ---------"
for M in modules/*; do
	M=$(basename $M)
	echo $M
	rsync --exclude=out --exclude=build -rin --ignore-existing --delete "$compare_with/modules/$M/src/main/groovy/com/inspeedia/common/$M/" "modules/$M/src/main/groovy/com/intellisrc/$M/"
	rsync --exclude=out --exclude=build -rin --ignore-existing --delete "$compare_with/modules/$M/src/test/groovy/com/inspeedia/common/$M/" "modules/$M/src/test/groovy/com/intellisrc/$M/"
done
echo -e "\e[39m"
