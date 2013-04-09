涉及的类:

org.h2.engine.Right

org.h2.engine.RightOwner
	<= org.h2.engine.Role
	<= org.h2.engine.User

org.h2.command.ddl.CreateRole
org.h2.command.ddl.CreateUser
org.h2.command.ddl.GrantRevoke

org.h2.command.ddl.DropRole
org.h2.command.ddl.DropUser

两种权限授予方式:
1. 将权限授予RightOwner
2. 将角色授予RightOwner

RightOwner可以是系统预定义的public角色，也可以是自定义的角色，还可以是用户

只支持SELECT、DELETE、INSERT、UPDATE总共4种权限(ALL表示所有权限都支持)



通过org.h2.engine.User.checkRight(Table, int)来做为权限检查的入口



DDL语句都需要admin权限




假设有如下三条语句:
GRANT myrole1 TO myrole3
GRANT myrole3 TO PUBLIC
GRANT myrole1 TO PUBLIC



当GRANT myrole1 TO myrole3时,grantedRole = myrole1, grantee = myrole3
会在myrole3.grantedRoles中保存myrole1

当GRANT myrole3 TO PUBLIC时,grantedRole = myrole3, grantee = PUBLIC
会在PUBLIC.grantedRoles中保存myrole3

当GRANT myrole1 TO PUBLIC时,grantedRole = myrole1, grantee = PUBLIC
因为PUBLIC.grantedRoles中已有myrole3，而myrole3.grantedRoles中又有myrole1，
所以此时“if (grantedRole != grantee && grantee.isRoleGranted(grantedRole))”为true，直接返回
    
org.h2.command.ddl.GrantRevoke.grantRole(Role)
==================================================
	private void grantRole(Role grantedRole) {
        if (grantedRole != grantee && grantee.isRoleGranted(grantedRole)) {
            return;
        }
        if (grantee instanceof Role) {
            Role granteeRole = (Role) grantee;
            if (grantedRole.isRoleGranted(granteeRole)) {
                // cyclic role grants are not allowed
                throw DbException.get(ErrorCode.ROLE_ALREADY_GRANTED_1, grantedRole.getSQL());
            }
        }
        Database db = session.getDatabase();
        int id = getObjectId();
        Right right = new Right(db, id, grantee, grantedRole);
        db.addDatabaseObject(session, right);
        grantee.grantRole(grantedRole, right);
    }

org.h2.engine.RightOwner.isRoleGranted(Role)
==================================================
    public boolean isRoleGranted(Role grantedRole) {
        if (grantedRole == this) {
            return true;
        }
        if (grantedRoles != null) {
            for (Role role : grantedRoles.keySet()) {
                if (role == grantedRole) {
                    return true;
                }

				//比如对应上面第三条GRANT语句，递归查看grantedRoles中的每个Roles是否被授予了grantedRole
                if (role.isRoleGranted(grantedRole)) {
                    return true;
                }
            }
        }
        return false;
    }
