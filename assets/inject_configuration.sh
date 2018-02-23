#! /bin/sh -

set -e

test_for_variable() {
  vname=$1
  eval "vvalue=\$$vname"

  if [ -z "$vvalue" ]
  then
    # No fallback has been given to the function
	echo "The environment variable ${vname} must have a value"
	exit 1
  fi
}

replace_variable_in() {
  #Usage: replace_variable_in VARIABLE_NAME file1 file2 ...
  vname=$1
  eval "vvalue=\$$vname"

  shift
  echo "Replace '{${vname}}' with '${vvalue}' in $@"
  sed -i "s/{${vname}}/${vvalue}/g" $@
}

fill_variables_in_war() {
  echo "Unpacking the .war"
  tmp_dir=/tmp/exploded_war
  war_file=$(basename $WAR_PATH)
  mkdir -p $tmp_dir && cd $tmp_dir

  unzip -qo $WAR_PATH

  echo "Setting environment variables: $@"
  for v in "$@" 
  do
    test_for_variable $v
    replace_variable_in $v WEB-INF/classes/*.*
  done
  echo "repacking the .war"

  zip -qr ../$war_file . \
    && mv ../$war_file $WAR_PATH \
    && cd - \
    && rm -rf $tmp_dir
}

if [ "$#" -ne 1 ]
then
  echo "Usage: $0 path/to/package_file.war"
  exit 2
fi

WAR_PATH=$1

eval set -- "DB_HOST \
	DB_PORT \
	TSDB_HOST \
	TSDB_PORT \
	C3P0_ACQUIRE_INCREMENT \
	C3P0_MAX_SIZE \
	C3P0_IDLE_TEST_PERIOD \
	C3P0_MAX_STATEMENTS \
	C3P0_MIN_SIZE \
	C3P0_TIMEOUT"

# Apply configuration
fill_variables_in_war $@

