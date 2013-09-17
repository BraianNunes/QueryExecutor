/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package queryexecutor;

public class UpdateResult {
    public final int affectedRows;

    public UpdateResult(int affectedRows) {
        this.affectedRows = affectedRows;
    }
}
