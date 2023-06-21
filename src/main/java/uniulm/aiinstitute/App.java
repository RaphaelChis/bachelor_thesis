package uniulm.aiinstitute;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.AutoIRIMapper;

/**
 * This App class contains the algorithm for the extraction of necessary 
 * knowledge from the configuration ontology and the generation of HTN planning 
 * problem descriptions.
 */
public class App 
{

    // Member variables
    private static OWLOntology mOntology = null;
    private static OWLReasoner mReasoner = null;

    /**
     * Main method where the domain and problem definition are defined. For the
     * definition, this method further utilizes helper methods.
     * 
     * @param args
     * @throws OWLOntologyCreationException
     * @throws OWLOntologyStorageException
     * @throws FileNotFoundException
     */
    public static void main( String[] args ) throws OWLOntologyCreationException, OWLOntologyStorageException, FileNotFoundException
    {

        // Load ontology
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        File folder = new File("src/main/resources");
        AutoIRIMapper mapper=new AutoIRIMapper(folder, true);
        manager.addIRIMapper(mapper);

        File file = new File("src/main/resources/config-leticia-ds.owl");
        FileDocumentSource fsource = new FileDocumentSource(file, new OWLXMLDocumentFormat());
        mOntology = manager.loadOntologyFromOntologyDocument(fsource);
        
        // Create reasoner
        OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
        mReasoner = reasonerFactory.createReasoner(mOntology);

        // Output domain definition
        // Output of SHOP methods and operators in proper methods
        for (OWLClass cls : mOntology.getClassesInSignature()) 
        {    

            if  (cls.getIRI().getFragment().equals("DomainDefinition"))
            {

                NodeSet<OWLNamedIndividual> domainDefinitions = mReasoner.getInstances(cls, true);
            
                for (OWLNamedIndividual i : domainDefinitions.getFlattened()) 
                {
                    
                    System.out.print("(defdomain " + getLabel(i) + "(");
                    
                    ArrayList<OWLNamedIndividual> methods = obtainObjPropAssertsOfProp(i, "hasMethod");
                    
                    for(OWLNamedIndividual method : methods)
                    {
                        outputMethod(method);
                    }

                    ArrayList<OWLNamedIndividual> operators = obtainObjPropAssertsOfProp(i, "hasOperator");
                    
                    for(OWLNamedIndividual operator : operators)
                    {
                        outputOperator(operator);
                    }
                    
                    System.out.println("))");

                }

            }

            // Output problem definition
            // Output of initial state and goal in proper methods
            if  (cls.getIRI().getFragment().equals("ProblemDefinition"))
            {

                NodeSet<OWLNamedIndividual> problemDefinitions = mReasoner.getInstances(cls, true);
            
                for (OWLNamedIndividual i : problemDefinitions.getFlattened()) 
                {
                    String dom = null;
                    ArrayList<OWLNamedIndividual> domains = obtainObjPropAssertsOfProp(i, "hasDomain");
                    
                    for(OWLNamedIndividual domain : domains)
                    {
                        dom = getLabel(domain);
                    }

                    System.out.println("(defproblem " + getLabel(i) + " " + dom);

                    ArrayList<OWLNamedIndividual> inits = obtainObjPropAssertsOfProp(i, "hasInitialState");
                    
                    for(OWLNamedIndividual init : inits)
                    {
                        outputInit(init);
                    }

                    ArrayList<OWLNamedIndividual> goals = obtainObjPropAssertsOfProp(i, "hasGoal");
                    
                    for(OWLNamedIndividual goal : goals)
                    {
                        outputGoal(goal);
                    }
                    
                    System.out.println(")");

                    
                }

            }

        }
        
    }

    /**
     * This method returns the assertions to the object property str of the individual i
     * in the array list inds.
     * 
     * @param i
     * @param str
     * @return inds
     */
    public static ArrayList<OWLNamedIndividual> obtainObjPropAssertsOfProp(OWLNamedIndividual i, String str)
    {
        ArrayList<OWLNamedIndividual> inds = new ArrayList<OWLNamedIndividual>();
        Set<OWLObjectPropertyAssertionAxiom> props = mOntology.getObjectPropertyAssertionAxioms(i);
                    
        for(OWLObjectPropertyAssertionAxiom prop: props)
        {
            OWLObjectPropertyExpression temp = prop.getProperty();
            String strtemp = temp.toString();
            String pr = strtemp.substring(strtemp.indexOf("#") + 1, strtemp.indexOf(">"));

            if(pr.equals(str))
            {
                OWLIndividual tempo = prop.getObject();
                OWLNamedIndividual ind = tempo.asOWLNamedIndividual();
                inds.add(ind);
            }
        }

        return inds;
    }
    
    /**
     * This method outputs the definitions of the method flows of individual i.
     * 
     * @param i
     */
    public static void outputMethod(OWLNamedIndividual i)
    {
        ArrayList<OWLNamedIndividual> flows = obtainObjPropAssertsOfProp(i, "hasFlow");

        for(OWLNamedIndividual flow : flows)
        {
            System.out.println();
            System.out.print("(:method (");
            
            System.out.print(getLabel(i));
            
            outputParams(i, "meHasParameter");
            System.out.println(")");

            outputPreconds(flow, "mfHasPrecondition");

            // Switch the order of "hasPostconditionMe" and "hasPostconditionOp"
            // for the recursive version. If not switched, the recursive method is applied
            // before the operator and there is no change in the current state.
            // Therefore next iteration, the same method is applied first again and so on.
            // The result is an infinite loop.
            System.out.print("(");
            outputPostconds(flow, "hasPostconditionMe");
            outputPostconds(flow, "hasPostconditionOp");
            System.out.print("))");

            System.out.println();
        }
    }

    /**
     * This method outputs the definition of operator individual i.
     * 
     * @param i
     */
    public static void outputOperator(OWLNamedIndividual i)
    {
        
        System.out.println();
        System.out.print("(:operator (!");
        
        System.out.print(getLabel(i));
        
        outputParams(i, "opHasParameter");
        System.out.println(")");

        outputPreconds(i, "opHasPrecondition");

        System.out.print("(");
        outputPostconds(i, "deletesPredicate");
        System.out.println(")");

        System.out.print("(");
        outputPostconds(i, "addsPredicate");
        System.out.print("))");

        System.out.println();
    }

    /**
     * This method outputs the preconditions of the individual i.
     * 
     * @param i
     * @param str
     */
    public static void outputPreconds(OWLNamedIndividual i, String str)
    {
        System.out.print("(");
        ArrayList<OWLNamedIndividual> preconds = obtainObjPropAssertsOfProp(i, str);
        
        // The first for-loop is only considered for recursive planning problems.
        // The preconditions labeled with the prefix "predicate-" have a parameter with 
        // the same name. The prefix "predicate-" is hence necessary as a distinction.
        // Such a precondition is for example "predicate-concept ?concept". These 
        // preconditions are stated at the beginning of the precondition listing, since their 
        // purpose is to filter out the objects of the corresponding types to which the 
        // following preconditions must apply for the method or operator to be triggered.
        for(OWLNamedIndividual precond : preconds)
        {
            
            String label = getLabel(precond);
            String labelsplt[] = label.split("-");
            if(labelsplt[0].equals("predicate"))
            {
                outputPred(precond);
            }
        }
        for(OWLNamedIndividual precond : preconds)
        {
            String label = getLabel(precond);
            String labelsplt[] = label.split("-");
            if(!(labelsplt[0].equals("predicate")))
            {
                outputPred(precond);
            }
        }
        System.out.println(")");
    }

    /**
     * This method outputs the postconditions of the individual i.
     * 
     * @param i
     * @param str
     */
    public static void outputPostconds(OWLNamedIndividual i, String str)
    {
        
        ArrayList<OWLNamedIndividual> postconds = obtainObjPropAssertsOfProp(i, str);
        for(OWLNamedIndividual postcond : postconds)
        {
            // The postconditions can be operator calls or method calls for methods or 
            // the deletion or addition of predicates for operators
            if(str.equals("hasPostconditionOp"))
            {
                outputOp(postcond);
            }
            else if (str.equals("hasPostconditionMe"))
            {
                outputMet(postcond);
            }
            else
            {
                outputPred(postcond);
            }
        }
        
    }

    /**
     * This method outputs the predicate individual i. 
     *
     * @param i
     */
    public static void outputPred(OWLNamedIndividual i) 
    {
        System.out.print("(");
        String temp = getLabel(i);
        
        // Check if predicate is positive
        ArrayList<OWLNamedIndividual> neg = obtainObjPropAssertsOfProp(i, "negativeOf");
        if(neg.size() == 0)
        {
            // Check if predicate is not a specialization
            ArrayList<OWLNamedIndividual> spec = obtainObjPropAssertsOfProp(i, "prSpecializationOf");
            if(spec.size() == 0)
            {
                
                System.out.print(temp);
                outputParams(i, "prHasParameter");
                System.out.print(")");
            }
            else
            {

                String comment = getComment(i);
                String commentsplit[] = comment.split(":");
                String paramOrObj = commentsplit[0];
                temp = getLabel(spec.get(0));

                if(paramOrObj.equals("Objects"))
                {
                    System.out.print(temp);
                    outputObjects(i, "prHasObject");
                    System.out.print(")");
                }
                else if(paramOrObj.equals("Parameters"))
                {
                    System.out.print(temp);
                    outputParams(i, "prHasParameter");
                    System.out.print(")");
                }
                else if(paramOrObj.equals("Mixed"))
                {
                    System.out.print(temp);
                    outputMixed(i);
                    System.out.print(")");
                } 

            }
        }
        else
        {
            System.out.print("not(");
            i = neg.get(0);

            ArrayList<OWLNamedIndividual> spec = obtainObjPropAssertsOfProp(i, "prSpecializationOf");
            if(spec.size() == 0)
            {
                temp = getLabel(i);
                System.out.print(temp);
                outputParams(i, "prHasParameter");
                System.out.print(")");
            }
            else
            {
                String comment = getComment(i);
                String commentsplit[] = comment.split(":");
                String paramOrObj = commentsplit[0];
                temp = getLabel(spec.get(0));

                if(paramOrObj.equals("Objects"))
                {
                    System.out.print(temp);
                    outputObjects(i, "prHasObject");
                    System.out.print(")");
                }
                else if(paramOrObj.equals("Parameters"))
                {
                    System.out.print(temp);
                    outputParams(i, "prHasParameter");
                    System.out.print(")");
                }
                else if(paramOrObj.equals("Mixed"))
                {
                    System.out.print(temp);
                    outputMixed(i);
                    System.out.print(")");
                } 
            }
            System.out.print(")");
        }
          
    }

    /**
     * This method outputs the initial state of problem definition individual i.
     * 
     * @param i
     */
    public static void outputInit(OWLNamedIndividual i)
    {
        System.out.print("(");
        ArrayList<OWLNamedIndividual> predicates = obtainObjPropAssertsOfProp(i, "hasPredicate");
        for(OWLNamedIndividual predicate : predicates)
        {
            outputPred(predicate);
        }
        System.out.println(")");
    }

    /**
     * This method outputs the goal of problem definition individual i.
     * 
     * @param i
     */
    public static void outputGoal(OWLNamedIndividual i)
    {
        
        ArrayList<OWLNamedIndividual> methods = obtainObjPropAssertsOfProp(i, "consistsOfMethod");
        for(OWLNamedIndividual method : methods)
        {
            outputMet(method);
        }
        
    }

    /**
     * This method outputs the call of SHOP method individual i.
     * 
     * @param i
     */
    public static void outputMet(OWLNamedIndividual i) 
    {
        System.out.print("(");
        String temp = getLabel(i);

        // Check if method is not a specialization
        ArrayList<OWLNamedIndividual> spec = obtainObjPropAssertsOfProp(i, "meSpecializationOf");
        if(spec.size() == 0)
        {
            
            System.out.print(temp);
            outputParams(i, "meHasParameter");
            System.out.print(")");
        }
        else
        {
            temp = getLabel(spec.get(0));
            System.out.print(temp);
            outputObjects(i, "meHasObject");
            System.out.print(")");
        }  
    }

    /**
     * This method outputs the call of SHOP operator individual i.
     * 
     * @param i
     */
    public static void outputOp(OWLNamedIndividual i) 
    {
        System.out.print("(!");
        String temp = getLabel(i);

        // Check if operator is not a specialization
        ArrayList<OWLNamedIndividual> spec = obtainObjPropAssertsOfProp(i, "opSpecializationOf");
        if(spec.size() == 0)
        {
            
            System.out.print(temp);
            outputParams(i, "opHasParameter");
            System.out.print(")");
        }
        else
        {
            temp = getLabel(spec.get(0));
            System.out.print(temp);
            outputObjects(i, "opHasObject");
            System.out.print(")");
        }
        
    }

    /**
     * This method outputs the parameters of the individual i.
     * 
     * @param i
     * @param str
     */
    public static void outputParams(OWLNamedIndividual i, String str)
    {
        ArrayList<OWLNamedIndividual> params = obtainObjPropAssertsOfProp(i, str);

        if(params.size() >= 1)
        {
            String comment = getComment(i);
            String list = comment.substring(comment.indexOf("Parameters: [")+13, comment.indexOf("]"));
            String[] arr = list.split(";");
            for(int j = 0; j < arr.length; j++)
            {
                String param = " ?" + arr[j];
                System.out.print(param);
            }
        }

    }

    /**
     * This method outputs the objects of the individual i. 
     * 
     * @param i
     * @param str
     */
    public static void outputObjects(OWLNamedIndividual i, String str)
    {
        ArrayList<OWLNamedIndividual> obs = obtainObjPropAssertsOfProp(i, str);
        
        if(obs.size() >= 1)
        {
            String comment = getComment(i);
            String list = comment.substring(comment.indexOf("Objects: [")+10, comment.indexOf("]"));
            String[] arr = list.split(";");
            for(int j = 0; j < arr.length; j++)
            {
                String obj = " " + arr[j];
                System.out.print(obj);
            }
        }    

    }

    /**
     * This method outputs the parameters and objects of the individual i.
     * 
     * @param i
     */
    public static void outputMixed(OWLNamedIndividual i)
    {
        
        String comment = getComment(i);
        String list = comment.substring(comment.indexOf("Mixed: [")+8, comment.indexOf("]"));
        String[] arr = list.split(";");
        for(int j = 0; j < arr.length; j++)
        {
            String obj = " " + arr[j];
            System.out.print(obj);
        } 

    }

    /**
     * This method returns the label of the individual i.
     * 
     * @param i
     * @return sol
     */
    public static String getLabel(OWLNamedIndividual i)
    {
        IRI iri = i.getIRI();
        String sol = null;
        for(OWLAnnotationAssertionAxiom a : mOntology.getAnnotationAssertionAxioms(iri)) 
        {
            if(a.getProperty().isLabel()) 
            {
                if(a.getValue() instanceof OWLLiteral) 
                {
                    OWLLiteral val = (OWLLiteral) a.getValue();
                    sol = val.getLiteral();
                }
            }
        }
        return sol;
    }

    /**
     * This method returns the comment of the individual i.
     * 
     * @param i
     * @return sol
     */
    public static String getComment(OWLNamedIndividual i)
    {
        IRI iri = i.getIRI();
        String sol = null;
        for(OWLAnnotationAssertionAxiom a : mOntology.getAnnotationAssertionAxioms(iri)) 
        {
            if(a.getProperty().isComment()) 
            {
                if(a.getValue() instanceof OWLLiteral) 
                {
                    OWLLiteral val = (OWLLiteral) a.getValue();
                    sol = val.getLiteral();
                }
            }
        }
        return sol;
    }
}