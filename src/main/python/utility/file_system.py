import errno
import os


def make_sure_path_exists(path):
    """Make sure that /some/directory in path to file /some/directory/file is created, do nothing if it exists.
:param path: Path to a file, all parent directories will be created.
"""
    try:
        os.makedirs( os.path.dirname( path ) )
    except OSError, exception: # need comma here, 'as' does not work in jython
        if exception.errno != errno.EEXIST:
            raise


def create_with_counter_if_existing( path, count = 0 ):
    """Create directory, if not existing, otherwise increase counter and append count to directory name and create.
:param path: Path to directory to be created.
:param count: Count to be increased, if path exists

:returns: Path of the directory that has been created.
:rtype: str
"""
    newPath = path.rstrip('/')
    if count != 0:
        newPath = newPath + '-' + str(count)
    try:
        os.makedirs( newPath )
        return newPath
    except OSError, exception:
        if exception.errno == errno.EEXIST:
            return create_with_counter_if_existing( path, count + 1 )
        else:
            raise
