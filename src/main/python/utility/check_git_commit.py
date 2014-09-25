import subprocess
import os

def getCommit( repoDir ):

    cwd         = os.getcwd()
    bashCommand = 'git rev-parse HEAD'

    os.chdir( repoDir )

    process = subprocess.Popen( bashCommand.split(), stdout=subprocess.PIPE )
    output  = process.communicate()[0]
    
    os.chdir( cwd )

    return repoDir



if __name__ == "__main__":
    print getCommit( '/groups/saalfeld/home/hanslovskyp/workspace/em-thickness-estimation' )

