#!/opt/bin/perl5
#
# This script is designed to walk through the ganymede package
# and make all the build scripts.

use Cwd;

#########################################################################
#
#                                                                 resolve
#
# input: $dir - absolute pathname of current directory
#        $link - string containing the readlink() results for a 
#                symbolic link in $dir to be processed
#
# returns: absolute pathname of the target of the symbolic link
#
#########################################################################
sub resolve{
    my($dir, $link) = @_;
    my(@alinkp, $d, $alinkp);

    # make array representations of
    # the current directory and symbolic link

    # if we have a leading / in our $dir or $link,
    # we'll need to shift to get rid of the leading
    # empty array element
  
    @dirp=split(/\//, $dir);
    shift(@dirp) if (!($dirp[0]));
    @linkp=split(/\//, $link);
    shift(@linkp) if (!($linkp[0]));

    # @alinkp is an array that we will build to contain the absolute link
    # target pathname.  If the link does not begin with a /, it is a relative link,
    # and we need to place our current directory into the @alinkp array.  

    if ($link !~ /^\//) {
	@alinkp=@dirp;
    }

    # modify the @alinkp array according
    # to each path component of the @linkp array
    # (an array representation of the symbolic link
    # given to us), to arrive at the ultimate absolute
    # pathname of the symbolic link

    $d = shift(@linkp);

    while ($d) {
	if ($d eq "..") {
	    pop(@alinkp);
	}
	elsif ($d ne "."){		       
	    push(@alinkp, $d);
	}
	$d=shift(@linkp);
    }

    $"='/';

    # perl functions return the value of the last expression
    # in the subroutine

    $alinkp="/@alinkp";
}

#
# Let's do it, then.
#

$perlname = $ENV{GPERL};
$rootdir = &resolve(cwd(), $ENV{GROOTDIR});

print "Hi, I'm your perl friend.  My perl is $perlname, my root is $rootdir\n";

@templates=("src/JCalendar/build.in",
	    "src/JDataComponent/build.in",
	    "src/JDialog/build.in",
	    "src/JTable/build.in",
	    "src/JTree/build.in",
	    "src/Util/build.in",
	    "src/client/build.in",
	    "src/client/rebuild.in",
	    "src/clientBase/build.in",
	    "src/server/build.in",
	    "src/server/rebuild.in");

@configfiles=("
	"src/schemas/bsd/custom_src/build.in",
	"src/schemas/bsd/loader/source/build.in",
	"src/schemas/gash/custom_src/build.in",
	"src/schemas/gash/loader/source/build.in",
	"src/schemas/gasharl/custom_src/build.in",
	"src/schemas/gasharl/loader/source/build.in",
	"src/schemas/linux/custom_src/build.in",
	"src/schemas/linux/loader/source/build.in",
	"src/schemas/nisonly/custom_src/build.in",
	"src/schemas/nisonly/loader/source/build.in",
