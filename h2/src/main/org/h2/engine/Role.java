/*
 * Copyright 2004-2018 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.engine;

import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.table.Table;

/**
 * Represents a role. Roles can be granted to users, and to other roles.
 */
public class Role extends RightOwner {

    private final boolean system;

    //org.h2.engine.Database.open(int, int)里预定义了一个publicRole = new Role(this, 0, Constants.PUBLIC_ROLE_NAME, true);
    //Constants.PUBLIC_ROLE_NAME="PUBLIC"
    public Role(Database database, int id, String roleName, boolean system) {
        super(database, id, roleName, Trace.USER);
        this.system = system;
    }

    @Override
    public String getCreateSQLForCopy(Table table, String quotedName) {
        throw DbException.throwInternalError(toString());
    }

    @Override
    public String getDropSQL() {
        return null;
    }

    /**
     * Get the CREATE SQL statement for this object.
     *
     * @param ifNotExists true if IF NOT EXISTS should be used
     * @return the SQL statement
     */
    public String getCreateSQL(boolean ifNotExists) {
        if (system) { //system角色不需要生成create语句，在org.h2.engine.Database.open(int, int)里会自动建立
            return null;
        }
        StringBuilder buff = new StringBuilder("CREATE ROLE ");
        if (ifNotExists) {
            buff.append("IF NOT EXISTS ");
        }
        buff.append(getSQL());
        return buff.toString();
    }

    @Override
    public String getCreateSQL() {
        return getCreateSQL(false);
    }

    @Override
    public int getType() {
        return DbObject.ROLE;
    }
    
    //dorp role时会调用
    //删除权限(类似于调用revoke命令)
    @Override
    public void removeChildrenAndResources(Session session) {
    	//与一个role相关的权限有三种
    	//前两种是此role被动授予给其他RightOwner的(包括用户和其他角色)
    	//另一种是授予给此role自己的
    	
    	//此role被授予给哪些user了，那们这些user要把此权限删了
        for (User user : database.getAllUsers()) {
            Right right = user.getRightForRole(this);
            if (right != null) {
            	//此方法内部会调用Right的removeChildrenAndResources，然后会触发User的revokeRole
                database.removeDatabaseObject(session, right);
            }
        }
        
        //此role被授予给哪些Role了，那们这些Role要把此权限删了
        for (Role r2 : database.getAllRoles()) {
            Right right = r2.getRightForRole(this);
            if (right != null) {
            	//此方法内部会调用Right的removeChildrenAndResources，然后会触发Role的revokeRole
                database.removeDatabaseObject(session, right);
            }
        }
        
        //授予给此role自己的权限要删除
        for (Right right : database.getAllRights()) {
            if (right.getGrantee() == this) {
            	//此方法内部会调用Right的removeChildrenAndResources，然后会触发Role的revokeRole
                database.removeDatabaseObject(session, right);
            }
        }
        database.removeMeta(session, getId());
        invalidate();
    }

    @Override
    public void checkRename() {
        // ok
    }

}
