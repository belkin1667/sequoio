package ru.sequoio.library.services.db.application.sieve;

import ru.sequoio.library.domain.Migration;
import ru.sequoio.library.domain.RunStatus;
import ru.sequoio.library.domain.migration_paramters.RunParameterValue;

public class RunSieve implements Sieve<Migration> {

    /**
     * Must match the following 'run status' and 'run modifier' matrix:     <br/><br/>

                 NEW BODY_CHANGED APPLIED                                   <br/>
            ONCE  +      ?           -                                      <br/>
          ALWAYS  +      +           +                                      <br/>
        ONCHANGE  +      +           -                                      <br/><br/>

     * Where '+' - to be applied                                            <br/>
     *       '-' - not to be applied                                        <br/>
     *       '?' - exceptional situation
    */
    @Override
    public boolean sift(Migration migration) {
        if (migration.getRunStatus() == null) {
            throw new IllegalStateException("Migration Log must be linked first!");
        }

        boolean isNew = RunStatus.NEW.equals(migration.getRunStatus());
        boolean isBodyChanged = RunStatus.BODY_CHANGED.equals(migration.getRunStatus());
        boolean isApplied = RunStatus.APPLIED.equals(migration.getRunStatus());
        boolean isAlways = RunParameterValue.ALWAYS.equals(migration.getRunModifier());
        boolean isOnChange = RunParameterValue.ONCHANGE.equals(migration.getRunModifier());
        if (isNew || isAlways || (isOnChange && isBodyChanged)) {
            return true;
        }
        else if (isApplied) {
            return false;
        } else {
            throw new IllegalArgumentException("Trying to modify migration body which was not marked as 'run:onchange' or 'run:always'");
        }
    }
}
