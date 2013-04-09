org.h2.table.TableView.getBestPlanItem(Session, int[])
	=> org.h2.index.ViewIndex.getCost(Session, int[])
	=> org.h2.index.ViewIndex.ViewIndex(TableView, ViewIndex, Session, int[])
		=> org.h2.index.ViewIndex.getQuery(Session, int[])

=> org.h2.index.ViewIndex.find(Session, SearchRow, SearchRow)


