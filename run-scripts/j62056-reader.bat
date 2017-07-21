::BATCH file to windows

set BATDIR=%~dp0
set LIBDIR=%BATDIR%..\build\libs-all

java -Djava.ext.dirs=%LIBDIR% org.openmuc.j62056.app.Reader %*
