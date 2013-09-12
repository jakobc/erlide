/**
 * 
 */
package org.erlide.engine.model.erlang;

import org.erlide.engine.model.IParent;

/**
 * @author jakob
 * 
 */
public interface IErlPreprocessorDef extends IErlMember, IParent {

    /**
     * @return the defined name of the macro or record
     */
    public String getDefinedName();

    /**
     * @return the macro or record body as string
     */
    public String getExtra();
}
