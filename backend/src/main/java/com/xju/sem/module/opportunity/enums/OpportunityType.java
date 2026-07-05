package com.xju.sem.module.opportunity.enums;

/** 机会类型（opportunity.type，schema 注释：COMPETITION/INNOVATION/INTERNSHIP/LECTURE）。 */
public enum OpportunityType {
    COMPETITION,
    INNOVATION,
    INTERNSHIP,
    LECTURE;

    public static boolean isValid(String v) {
        if (v == null) {
            return false;
        }
        for (OpportunityType t : values()) {
            if (t.name().equals(v)) {
                return true;
            }
        }
        return false;
    }
}
