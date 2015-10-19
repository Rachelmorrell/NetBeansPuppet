
package com.tropyx.nb_puppet.hiera;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.AuxiliaryProperties;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.yaml.snakeyaml.Yaml;

public class HieraYamlProvider {

    public static @CheckForNull Data getHieraYaml(@NonNull Project p) {

        AuxiliaryProperties aux = p.getLookup().lookup(AuxiliaryProperties.class);
        if (aux == null) {
            return null;
        }
        String hiera = aux.get(HieraPanel.HIERALOCATION, true);
        if (hiera == null) {
            hiera = "hiera.yaml";
        }
        FileObject hierfo = p.getProjectDirectory().getFileObject(hiera);
        if (hierfo == null) {
            return null;
        }
        List<String> hierarchy = Collections.emptyList();
        List<Backend> backends = new ArrayList<>();
        Yaml yaml = new Yaml();
        try {
            Map<String, Object> root = (Map<String, Object>) yaml.load(hierfo.getInputStream());
            Object b = root.get(":backends");
            List<String> backs = Collections.emptyList();
            if (b instanceof String) {
                backs = Collections.singletonList((String)b);
            } else if (b instanceof List) {
                backs = new ArrayList<>((List)b);
            }
            Object hier = root.get(":hierarchy");
            if (hier instanceof List) {
                hierarchy = new ArrayList<>((List)hier);
            }
            for (String backend : backs) {
                Map<String, Object> backendNode = (Map<String, Object>) root.get(":" + backend);
                if (backendNode != null) {
                    String datadir = (String) backendNode.get(":datadir");
                    if ("eyaml".equals(backend)) {
                        String privateKey = (String) backendNode.get(":pkcs7_private_key");
                        String publicKey = (String) backendNode.get(":pkcs7_public_key");
                        String extension = (String) backendNode.get(":extension");
                        extension = extension == null ? "eyaml" : extension;
                        backends.add(new EyamlBackend(backend, datadir, privateKey, publicKey, extension));
                    } else {
                       backends.add(new Backend(backend, datadir));
                    }
                }

            }
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
        return new Data(hierarchy, backends);
    }

    public static final class Data {
        final List<Backend> backends;
        final List<String> hierarchy;

        public Data(List<String> hierarchy, List<Backend> backends) {
            this.backends = backends;
            this.hierarchy = hierarchy;
        }

        public List<Backend> getBackends() {
            return backends;
        }

        public Backend getBackend(String type) {
            for (Backend b : backends) {
                if (type.equals(b.type)) {
                    return b;
                }
            }
            return null;
        }

        public List<String> getHierarchy() {
            return hierarchy;
        }

        @Override
        public String toString() {
            return "Data{" + "backends=" + backends + ", hierarchy=" + hierarchy + '}';
        }

    }

    public static class Backend {
        final String type;
        final String dataDir;
        public Backend(String type, String dataDir) {
            this.type = type;
            this.dataDir = dataDir;
        }

        public String getType() {
            return type;
        }

        public String getDataDir() {
            return dataDir;
        }

        @Override
        public String toString() {
            return "Backend{" + "type=" + type + ", dataDir=" + dataDir + '}';
        }

    }

    public static class EyamlBackend extends Backend {
        private final String privateKey;
        private final String publicKey;
        private final String extension;

        public EyamlBackend(String type, String dataDir, String privateKey, String publicKey, String extension) {
            super(type, dataDir);
            this.publicKey = publicKey;
            this.privateKey = privateKey;
            this.extension = extension;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public String getExtension() {
            return extension;
        }

        @Override
        public String toString() {
            return "EyamlBackend{" + "privateKey=" + privateKey + ", publicKey=" + publicKey + super.toString() + '}';
        }

    }
}
