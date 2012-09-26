package eu.compassresearch.core.typechecker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.compassresearch.ast.types.ABracketType;
import eu.compassresearch.ast.types.AIntNumericBasicType;
import eu.compassresearch.ast.types.ANamedInvariantType;
import eu.compassresearch.ast.types.ANatNumericBasicType;
import eu.compassresearch.ast.types.ANatOneNumericBasicType;
import eu.compassresearch.ast.types.AOptionalType;
import eu.compassresearch.ast.types.ARationalNumericBasicType;
import eu.compassresearch.ast.types.ARealNumericBasicType;
import eu.compassresearch.ast.types.ASeqSeqType;
import eu.compassresearch.ast.types.AUnknownType;
import eu.compassresearch.ast.types.PType;
import eu.compassresearch.ast.types.SBasicType;
import eu.compassresearch.core.typechecker.api.TypeComparator;

class SimpleTypeComparator implements TypeComparator
  {
    
    /*
     * 
     * Representation of a pair of types mostly used to return pairs
     */
    private static class TypePair
      {
        private static final int LARGEST_31_BITS_PRIME = 0x7FFFFFFF;
        private PType            from;
        private PType            to;
        
        public TypePair(PType from, PType to)
          {
            this.from = from;
            this.to = to;
          }
        
        @Override
        public int hashCode()
          {
            int fromHash = from.hashCode();
            int toHash = to.hashCode();
            return ((fromHash + toHash) % LARGEST_31_BITS_PRIME);
          }
        
        @Override
        public boolean equals(Object obj)
          {
            if (!this.getClass().isInstance(obj))
              return false;
            TypePair other = this.getClass().cast(obj);
            return this.to.equals(other.to) && this.from.equals(other.from);
          }
        
        @Override
        public String toString()
          {
            return "(" + from + ", " + to + ")";
          }
        
      }
    
    /**
     * Construct a type comparator
     * 
     * @return
     */
    public static TypeComparator newInstance()
      {
        TypeComparator res = new SimpleTypeComparator(null);
        return res;
      }
    
    private Environment<PType> typeEnvironment;
    
    // only created through factory method
    private SimpleTypeComparator(Environment<PType> env)
      {
        this.typeEnvironment = env;
      }
    
    // PUBLIC Interface
    /*
     * (non-Javadoc)
     * 
     * @see
     * eu.compassresearch.core.typechecker.TypeComparator#compatible(java.util
     * .List, java.util.List)
     */
    @Override
    public boolean compatible(List<PType> to, List<PType> from)
      {
        // check size
        if (to.size() != from.size())
          return false;
        
        int i = 0;
        
        for (i = 0; i < to.size(); i++)
          {
            boolean c = compatible(to.get(i), from.get(i));
            if (!c)
              return false; // the i'th pair are not compatible
          }
        return true; // all pairs are compatible
      }
    
    /*
     * (non-Javadoc)
     * 
     * @see eu.compassresearch.core.typechecker.TypeComparator#compatible(eu.
     * compassresearch.ast.types.PType, eu.compassresearch.ast.types.PType)
     */
    @Override
    public boolean compatible(PType to, PType from)
      {
        TypePair realTypes = obtainFundamentalTypes(to, from);
        if (realTypes == null)
          return false;
        
        // they are the same type object
        if (to.equals(from))
          return true;
        
        // for basic type the class suffice
        if (to instanceof SBasicType && to.getClass().equals(from.getClass()))
          return true;
        
        return false;
      }
    
    /*
     * (non-Javadoc)
     * 
     * @see eu.compassresearch.core.typechecker.TypeComparator#compatible(eu.
     * compassresearch.ast.types.PType, eu.compassresearch.ast.types.PType,
     * boolean)
     */
    @Override
    public boolean compatible(PType to, PType from, boolean paramOnly)
      {
        return false;
      }
    
    private static final Map<Class<?>, List<Class<?>>> fixedSubTypeRelations;
    static
      {
        fixedSubTypeRelations = new HashMap<Class<?>, List<Class<?>>>();
        fixedSubTypeRelations.put(AIntNumericBasicType.class,
            Arrays.asList(new Class<?>[] { ANatNumericBasicType.class }));
        fixedSubTypeRelations.put(ANatNumericBasicType.class,
            Arrays.asList(new Class<?>[] { ANatOneNumericBasicType.class }));
        fixedSubTypeRelations.put(ARationalNumericBasicType.class,
            Arrays.asList(new Class<?>[] { AIntNumericBasicType.class }));
        fixedSubTypeRelations.put(
            ARealNumericBasicType.class,
            Arrays.asList(new Class<?>[] { AIntNumericBasicType.class,
        ARationalNumericBasicType.class }));
        
      }
    
    private static boolean checkClosureOnFixedTypeRelation(Class<?> top,
        Class<?> bottom)
      {
        if (top == bottom)
          return true;
        
        if (fixedSubTypeRelations.containsKey(top))
          {
            boolean f = false;
            for (Class<?> candidate : fixedSubTypeRelations.get(top))
              f |= checkClosureOnFixedTypeRelation(candidate, bottom);
            return f;
          }
        
        return false;
      }
    
    /*
     * (non-Javadoc)
     * 
     * @see eu.compassresearch.core.typechecker.TypeComparator#isSubType(eu.
     * compassresearch.ast.types.PType, eu.compassresearch.ast.types.PType)
     */
    @Override
    public boolean isSubType(PType sub, PType sup)
      {
        // un-pack types
        TypePair pair = obtainFundamentalTypes(sup, sub);
        
        // Basic or built-in types can be handled by this
        boolean fixedTypes = checkClosureOnFixedTypeRelation(
            pair.from.getClass(), pair.to.getClass());
        if (fixedTypes)
          return true;
        
        // sub type unknown
        if (pair.from instanceof AUnknownType
            || pair.to instanceof AUnknownType)
          return true;
        
        // TODO: sub type maps
        
        // TODO: sub type sets
        
        // sub type sequences
        if (pair.from instanceof ASeqSeqType)
          {
            if (pair.to instanceof ASeqSeqType)
              {
                PType innerFromType = ((ASeqSeqType) pair.from).getSeqof();
                PType innerToType = ((ASeqSeqType) pair.to).getSeqof();
                return isSubType(innerToType, innerFromType);
              }
            return false;
          }
        
        return fixedTypes;
      }
    
    // Un-pack types (copied from Overture)
    private TypePair obtainFundamentalTypes(PType to, PType from)
      {
        boolean resolved = false;
        
        while (!resolved)
          {
            if (to instanceof ABracketType)
              {
                to = ((ABracketType) to).getType();
                continue;
              }
            
            if (from instanceof ABracketType)
              {
                from = ((ABracketType) from).getType();
                continue;
              }
            
            if (to instanceof ANamedInvariantType)
              {
                to = ((ANamedInvariantType) to).getType();
                continue;
              }
            
            if (from instanceof ANamedInvariantType)
              {
                from = ((ANamedInvariantType) from).getType();
                continue;
              }
            
            if (to instanceof AOptionalType)
              {
                if (from instanceof AOptionalType)
                  {
                    resolved = true;
                    break;
                  }
                
                to = ((AOptionalType) to).getType();
                continue;
              }

            
            if (from instanceof AOptionalType)
              {
                // Can't assign nil to a non-optional type? This should maybe
                // generate a warning here?
                
                if (to instanceof AOptionalType)
                  {
                    resolved = true;
                    break;
                  }
                
                from = ((AOptionalType) from).getType();
                continue;
              }
            
            resolved = true;
          }
        
        if (resolved)
          return new TypePair(to, from);
        
        return null;
      }
  }