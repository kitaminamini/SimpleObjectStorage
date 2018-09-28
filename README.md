# SimpleObjectStorage

How to run it
1. On local: 

             a. change dbconfig to 'local' from 'db'
             b. run mongo
             c. build & run app
             
2. On Docker: 

              a. In project dir, run command "mvn clean package"
              b. copy the whole target dir to replace the one inside webapp dir
              c. "cd webapp"
              d. "docker build ."
              e. "cd .."
              f. "docker-compose up --build
