import org.omg.CosNaming.*;
import org.omg.CORBA.*;
import PDFModule.*;

public class ServeurPDF {
    public static void main(String args[]) {
        try {
            ORB orb = ORB.init(args, null);
            org.omg.PortableServer.POA rootpoa =
                org.omg.PortableServer.POAHelper.narrow(
                    orb.resolve_initial_references("RootPOA"));
            rootpoa.the_POAManager().activate();

            PDFServiceImpl impl = new PDFServiceImpl();
            org.omg.CORBA.Object ref =
                rootpoa.servant_to_reference(impl);
            PDFService href = PDFServiceHelper.narrow(ref);

            org.omg.CORBA.Object objRef =
                orb.resolve_initial_references("NameService");
            NamingContextExt ncRef =
                NamingContextExtHelper.narrow(objRef);

            NameComponent path[] = ncRef.to_name("PDFService");
            ncRef.rebind(path, href);

            System.out.println("=== Serveur PDF CORBA pret ===");
            orb.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
