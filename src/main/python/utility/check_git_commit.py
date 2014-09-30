import subprocess
import os

def getCommit( repoDir ):

    cwd         = os.getcwd()
    bashCommand = 'git rev-parse HEAD'

    os.chdir( repoDir )

    process = subprocess.Popen( bashCommand.split(), stdout=subprocess.PIPE )
    output  = process.communicate()[0].rstrip( '\n' )
    
    os.chdir( cwd )

    return output

def getDiff( repoDir ):

    cwd = os.getcwd()
    bashCommand = 'git diff'

    os.chdir( repoDir )

    process = subprocess.Popen( bashCommand.split(), stdout=subprocess.PIPE )
    output  = process.communicate()[0].rstrip( '\n' )

    os.chdir( cwd )

    return output



if __name__ == "__main__":
    print getCommit( '/groups/saalfeld/home/hanslovskyp/workspace/em-thickness-estimation' )

