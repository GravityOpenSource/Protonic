/*
 * This file renders the Info component for Bio Hansel
 */

import React from "react";
import { BasicList } from "../../../../components/lists/BasicList";

import { TabPaneContent } from "../../../../components/tabs/TabPaneContent";
import { Error } from "../../../../components/icons/Error";
import { Success } from "../../../../components/icons/Success";
import { Warning } from "../../../../components/icons/Warning";

export default function BioHanselInfo({ bioHanselResults }) {
  const biohanselResults = [
    {
      title: i18n("AnalysisBioHansel.sampleName"),
      desc: bioHanselResults ? bioHanselResults.sample : ""
    },
    {
      title: `${i18n("AnalysisBioHansel.schemeName")} (v${i18n(
        "AnalysisBioHansel.schemeVersion"
      )})`,
      desc: bioHanselResults
        ? `${bioHanselResults.scheme} (${bioHanselResults.scheme_version})`
        : ""
    },
    {
      title: i18n("AnalysisBioHansel.subtype"),
      desc: bioHanselResults ? bioHanselResults.subtype : ""
    },
    {
      title: i18n("AnalysisBioHansel.averageTileFrequency"),
      desc:
        bioHanselResults && bioHanselResults.avg_tile_coverage
          ? bioHanselResults.avg_tile_coverage.toString()
          : ""
    },
    {
      title: i18n("AnalysisBioHansel.qualityControlStatus"),
      desc: bioHanselResults ? (
        bioHanselResults.qc_status === "PASS" ? (
          <Success
            message={formatQcMessage(
              bioHanselResults.qc_status,
              bioHanselResults.qc_message
            )}
          />
        ) : bioHanselResults.qc_status === "FAIL" ? (
          <Error
            message={formatQcMessage(
              bioHanselResults.qc_status,
              bioHanselResults.qc_message
            )}
          />
        ) : (
          <Warning
            message={formatQcMessage(
              "WARNING",
              bioHanselResults.qc_message ? qc_message : ""
            )}
          />
        )
      ) : (
        ""
      )
    }
  ];

  function formatQcMessage(qc_status, qc_message) {
    let msgs = [];
    if (qc_message) {
      msgs = qc_message.trim().split("|");
    }

    return (
      <>
        <span>{qc_status}</span>
        <br />
        {msgs.length > 0 ? (
          <ul>
            {msgs.map(msg => {
              return <li key={msg}>{msg}</li>;
            })}
          </ul>
        ) : null}
      </>
    );
  }
  /*
   * Returns a simple list which displays labels and values
   * for the bio hansel data
   */
  return (
    <TabPaneContent title={i18n("AnalysisBioHansel.bioHanselInformation")}>
      <BasicList dataSource={biohanselResults}></BasicList>
    </TabPaneContent>
  );
}