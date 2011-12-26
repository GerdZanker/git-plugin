package hudson.plugins.git.browser;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.plugins.git.GitChangeSet;
import hudson.plugins.git.GitChangeSet.Path;
import hudson.scm.EditType;
import hudson.scm.RepositoryBrowser;
import hudson.scm.browsers.QueryBuilder;
import hudson.util.FormValidation;
import hudson.util.FormValidation.URLCheck;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class TracGitBrowser extends GitRepositoryBrowser {

    private static final long serialVersionUID = 1L;
    private final URL url;

    @DataBoundConstructor
    public TracGitBrowser(String url) throws MalformedURLException {
        this.url = normalizeToEndWithSlash(new URL(url));
    }


    private int getFileIndex(Path path) {
    	Collection<Path> affectedFiles = path.getChangeSet().getAffectedFiles();
    	Iterator<Path> it = affectedFiles.iterator();
    	int index = 0;
    	while(it.hasNext()) {
    		if(it.next() == path)
    			return index;
    		index++;
    	}
		return -1;
	}

    @Override
    public URL getDiffLink(Path path) throws IOException {
    	// returns <url>"/changeset/"<changesetID>"#file"<NoOfFileInChangeset>
    	// e.g. https://fedorahosted.org/eclipse-fedorapackager/changeset/0956859f7db2656cae445488689a214c104bf1b3#file3
        if (path.getEditType() == EditType.EDIT) {
        	return new URL(url, getChangeSetLink(path.getChangeSet()).toString() + "#file" + getFileIndex(path));            
        }
        return null;
    }


	@Override
    public URL getFileLink(Path path) throws IOException {
    	// returns <url>"/browser/"<file>"$rev="<changsetID>
    	// e.g. https://fedorahosted.org/eclipse-fedorapackager/browser/org.fedoraproject.eclipse.packager.rpm/src/org/fedoraproject/eclipse/packager/rpm/RpmText.java?rev=0956859f7db2656cae445488689a214c104bf1b3
        String spec;
        if (path.getEditType() == EditType.DELETE) {
        	spec = param().add("rev="+path.getChangeSet().getParentCommit()).toString();
        } else {
        	spec = param().add("rev="+path.getChangeSet().getId()).toString();
        }
        return new URL(url, url.getPath() + "browser/" + path.getPath() + spec);
    }

    @Override
    public URL getChangeSetLink(GitChangeSet changeSet) throws IOException {
    	// returns <url>"/changeset/"<changsetID>
    	// e.g. https://fedorahosted.org/eclipse-fedorapackager/changeset/0956859f7db2656cae445488689a214c104bf1b3
        return new URL(url, url.getPath() + "changeset/" + changeSet.getId());
    }

    private QueryBuilder param() {
        return new QueryBuilder(url.getQuery());
    }

    public URL getUrl() {
        return url;
    }
    
    
    @Extension
    public static class TracGitBrowserDescriptor extends Descriptor<RepositoryBrowser<?>> {
        public String getDisplayName() {
            return "tracGit";
        }

        @Override
        public TracGitBrowser newInstance(StaplerRequest req, JSONObject jsonObject) throws FormException {
            return req.bindParameters(TracGitBrowser.class, "tracgit.");
        }

        public FormValidation doCheckUrl(@QueryParameter(fixEmpty = true) final String url) throws IOException, ServletException {
            if (url == null) // nothing entered yet
                return FormValidation.ok();
            return new URLCheck() {
                protected FormValidation check() throws IOException, ServletException {
                    String v = url;
                    if (!v.endsWith("/"))
                        v += '/';
                    return FormValidation.ok();
                }
            }.check();
        }
    }
}
