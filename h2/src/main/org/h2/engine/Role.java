/*
 * Copyright 2004-2021 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.engine;

import java.util.ArrayList;

import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.schema.Schema;
import org.h2.table.Table;

/**
 * Represents a role. Roles can be granted to users, and to other roles.
 */
public final class Role extends RightOwner {

    private final boolean system;

    //org.h2.engine.Database.open(int, int)里预定义了一个publicRole = new Role(this, 0, Constants.PUBLIC_ROLE_NAME, true);
    //Constants.PUBLIC_ROLE_NAME="PUBLIC"
    public Role(Database database, int id, String roleName, boolean system) {
        super(database, id, roleName, Trace.USER);
        this.system = system;
    }

    @Override
    public String getCreateSQLForCopy(Table table, String quotedName) {
        throw DbException.getInternalError(toString());
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
        StringBuilder builder = new StringBuilder("CREATE ROLE ");
        if (ifNotExists) {
            builder.append("IF NOT EXISTS ");
        }
        return getSQL(builder, DEFAULT_SQL_FLAGS).toString();
    }

    @Override
    public String getCreateSQL() {
        return getCreateSQL(false);
    }

    @Override
    public int getType() {
        return DbObject.ROLE;
    }
    
    // dorp role时会调用
    // 删除权限(类似于调用revoke命令)

    // 与一个role相关的权限有三种
    // 前两种是此role被动授予给其他RightOwner的(包括用户和其他角色)
    // 另一种是授予给此role自己的

    // 此role被授予给哪些user了，那们这些user要把此权限删了
    @Override
//<<<<<<< HEAD
//    public void removeChildrenAndResources(SessionLocal session) {
//        for (User user : database.getAllUsers()) {
//            Right right = user.getRightForRole(this);
//            if (right != null) {
//            	//此方法内部会调用Right的removeChildrenAndResources，然后会触发User的revokeRole
//                database.removeDatabaseObject(session, right);
//            }
//        }
//        
//        //此role被授予给哪些Role了，那们这些Role要把此权限删了
//        for (Role r2 : database.getAllRoles()) {
//            Right right = r2.getRightForRole(this);
//=======
    public ArrayList<DbObject> getChildren() {
        ArrayList<DbObject> children = new ArrayList<>();
        for (Schema schema : database.getAllSchemas()) {
            if (schema.getOwner() == this) {
                children.add(schema);
            }
        }
        return children;
    }

    @Override
    public void removeChildrenAndResources(SessionLocal session) {
        for (RightOwner rightOwner : database.getAllUsersAndRoles()) {
            Right right = rightOwner.getRightForRole(this);
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

}
