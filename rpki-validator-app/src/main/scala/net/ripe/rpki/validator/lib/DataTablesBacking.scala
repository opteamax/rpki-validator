/**
 * The BSD License
 *
 * Copyright (c) 2010, 2011 RIPE NCC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   - Neither the name of the RIPE NCC nor the names of its contributors may be
 *     used to endorse or promote products derived from this software without
 *     specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.ripe.rpki.validator
package lib

import org.scalatra.ScalatraKernel
import net.liftweb.json._
import net.ripe.ipresource.IpRange
import net.ripe.ipresource.Asn

import scala.NotDefinedError

trait DataTablesBacking[R <: Any] {

  protected def getParam(name: String): String
  protected def getAllRecords(): IndexedSeq[R]
  protected def filterRecords(records: IndexedSeq[R], searchCriterium: Any): IndexedSeq[R]
  protected def sortRecords(records: IndexedSeq[R], sortColumn: Int): IndexedSeq[R]
  protected def getValuesForRecord(record: R): List[String]

  val iDisplayStart = getParam("iDisplayStart").toInt
  val iDisplayLength = getParam("iDisplayLength").toInt
  val sSearch = getParam("sSearch").toUpperCase()

  val sortCol = getParam("iSortCol_0").toInt
  val sortOrder = getParam("sSortDir_0")

  def searchCriterium = {
    try {
      IpRange.parse(sSearch)
    } catch {
      case _ => try {
        if (sSearch.startsWith("AS")) {
          Asn.parse(sSearch)
        } else {
          sSearch
        }
      } catch {
        case _ => sSearch
      }
    }
  }

  def renderRecords() = {

    val allRecords = getAllRecords
    val filteredRecords = filterRecords(allRecords, searchCriterium)
    val sortedRecords = sortRecords(filteredRecords, sortCol)
    val orderedRecords = order(sortedRecords)
    val displayRecords = paginate(orderedRecords)

    compact(render(JObject(List(
      JField("sEcho", JInt(getParam("sEcho").toInt)),
      JField("iTotalRecords", JInt(allRecords.size)),
      JField("iTotalDisplayRecords", JInt(filteredRecords.size)),
      JField("aaData", makeJArray(displayRecords))))))
  }

  private def order(records: IndexedSeq[R]) = {
    sortOrder match {
      case "desc" => records.reverse
      case _ => records
    }
  }
  
  private def paginate(records: IndexedSeq[R]) = {
    records.drop(iDisplayStart).take(iDisplayLength)
  }

  private def makeJArray(records: IndexedSeq[R]): JArray = {
    JArray(
      records.map {
        record =>
          {
            JArray(makeJStringListForRecord(record))
          }
      }.toList)
  }

  private def makeJStringListForRecord(record: R): List[JValue] = {
    val strings = getValuesForRecord(record)
    strings.map {
      string => JString(string)
    }
  }

}