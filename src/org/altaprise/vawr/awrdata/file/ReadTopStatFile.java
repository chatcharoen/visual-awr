package org.altaprise.vawr.awrdata.file;


import dai.shared.businessObjs.DBAttributes;
import dai.shared.businessObjs.DBRec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.StringTokenizer;

import org.altaprise.vawr.awrdata.OSWData;
import org.altaprise.vawr.charts.oswatcher.TopStatTimeSeriesChart;


public class ReadTopStatFile {

    private BufferedReader _fileReader;
    private String _platformType = "";
    private String _topFileType = "";
    private String _fileName = null;


    public ReadTopStatFile() {

    }

    public static void main(String[] args) {
        try {
            ReadTopStatFile duFile = new ReadTopStatFile();
            //duFile.parse("C:/Git/visual-awr/testing/oswatcher/osc2cn01-d1_top_14.09.10.0400.dat");
            duFile.parse("C:/Git/visual-awr/testing/oswatcher/linux/em12c_top_14.09.12.0300.dat");
            OSWData.getInstance().dump();
            new TopStatTimeSeriesChart("", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DBRec testFile(String fileName, Date startDateFilter, Date endDateFilter, boolean doFilter) throws Exception {
        try {
            String rec = "";
            _fileReader = new BufferedReader(new FileReader(fileName));
            DBRec fileRecSet = null;

            try {
                //Priming read
                rec = _fileReader.readLine();
                Date lastDateTimeRead = null;
                Date firstDateTimeRead = null;
                boolean isValidFileFormat = false;
                
                while (rec != null) {
                    if (rec.startsWith("zzz ***")) {
                        try {
                            Date dateTimeD = this.parseDateTime(rec);
                            //All we are doing right now is validating that we can find 
                            //a record that starts with "zzz ***" and the time format validates
                            isValidFileFormat = true;
                            if (doFilter) {
                                if (dateTimeD.after(startDateFilter) && dateTimeD.before(endDateFilter)) {
                                    if (firstDateTimeRead == null) {
                                        firstDateTimeRead = dateTimeD;
                                    }
                                    lastDateTimeRead = dateTimeD;
                                }
                            } else {
                                if (firstDateTimeRead == null) {
                                    firstDateTimeRead = dateTimeD;
                                }
                                lastDateTimeRead = dateTimeD;
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                    }

                    //Read the next record
                    rec = _fileReader.readLine();
                }

                if (firstDateTimeRead != null) {
                    fileRecSet = new DBRec();
                    fileRecSet.addAttrib(new DBAttributes("FILE_START_DATETIME", firstDateTimeRead.toString()));
                    fileRecSet.addAttrib(new DBAttributes("FILE_END_DATETIME", lastDateTimeRead.toString()));
                } else {
                    //Let's make sure we found at least one valid record regarless of the filter
                    if (!isValidFileFormat) {
                        throw new Exception ("Did not find any record descriptors of type: \"zzz ***\"");
                    }
                }
                
                return fileRecSet;

            } catch (Exception e) {
                System.out.println("IOSTAT File Read Error\n" + e.getLocalizedMessage());
                e.printStackTrace();
                throw e;
            }

        } catch (FileNotFoundException fnfe) {
            throw new Exception("File Not Found: " + fileName);
        } catch (Exception e) {
            throw e;
        } finally {
            _fileReader.close();
        }
    }

    public void parse(String fileName) throws Exception {
        try {

            _fileReader = new BufferedReader(new FileReader(fileName));
            //Strip the path off.
            _fileName = fileName.substring(fileName.lastIndexOf(File.separator)+1, fileName.length());
            readMainMetrics();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
            throw new Exception("File Not Found: " + fileName);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (_fileReader != null) {
                _fileReader.close();
            }
        }
    }

    private void readMainMetrics() throws Exception {
        String rec = "";


        try {
            //Priming read
            rec = _fileReader.readLine();

            //Validate the type of file this is.  Is it OSWatcher?  Is it from Linux or Solaris?
            if (rec.toUpperCase().contains("LINUX")) {
                _platformType = "LINUX";
            } else if (rec.toUpperCase().contains("SUNOS")) {
                _platformType = "SUNOS";
            } else {
                _platformType = "UNKNOWN";
            }
            OSWData.getInstance().setPlatformType(_platformType);
            
            //Is this an OSW or Exawatcher file?
            if (rec.toUpperCase().contains("OSW")) {
                _topFileType = "OSW";
            } else {
                _topFileType = "UNKNOWN";
            }

            System.out.println("platformType/topFileType: " + _platformType + "/" + _topFileType);
            DBRec dbRec = null;
            boolean cpuRecFound = false;
            boolean memRecFound = false;

            while (rec != null) {

                if (rec.startsWith("zzz ***")) {
                    cpuRecFound = false;
                    memRecFound = false;
                    //Create the record structure
                    dbRec = new DBRec();
                    //Parse the datetime and add the attributes to the dbRec
                    Date recDateTime = this.parseDateTime(rec);
                    dbRec.addAttrib(new DBAttributes("FILE_NAME", _fileName));
                    dbRec.addAttrib(new DBAttributes("DATE", recDateTime));


                } else if (rec.startsWith("CPU") || rec.startsWith("Cpu")) {
                    cpuRecFound = true;
                    if (_platformType.equals("SUNOS")) {
                        this.parseSunCpuMetrics(rec, dbRec);
                    } else {
                        this.parseLinuxCpuMetrics(rec, dbRec);
                    }

                } else if (rec.startsWith("Mem")) {
                    memRecFound = true;
                    if (_platformType.equals("SUNOS")) {
                        this.parseSunMemoryMetrics(rec, dbRec);
                    } else {
                        this.parseLinuxMemoryMetrics(rec, dbRec);
                    }
                    //On some rare occasions there is not a CPU record in the Top stats.  
                    //We need to filter these out so that charting will work.
                    if (cpuRecFound && memRecFound) {
                        OSWData.getInstance().addTopStatRec(dbRec);
                    }
                } else {
                    //Do nothing
                }

                //Read the next record
                rec = _fileReader.readLine();
            }
        } catch (Exception e) {
            System.out.println("PropertyFileReader::readData\n" + e.getLocalizedMessage());
            e.printStackTrace();
            throw e;
        }
    }

    //zzz ***Wed Sep 10 04:00:08 PDT 2014
    private Date parseDateTime(String rec) {
        Date ret = null;
        if (rec.length() > 0) {

            String recDateTimeS = rec.substring(11, 35);
            //String intervalSecondsS = rec.substring(52, 53);

            try {
                //_intervalSeconds = Integer.parseInt(intervalSecondsS);
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd hh:mm:ss zzz yyyy");
                ret = dateFormat.parse(recDateTimeS);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return ret;
    }

    //SUNOS:  CPU states: 94.5% idle,  4.6% user,  0.9% kernel,  0.0% iowait,  0.0% swap
    //LINUX:  Cpu(s):  5.0%us,  2.5%sy,  0.0%ni, 92.4%id,  0.0%wa,  0.0%hi,  0.0%si,  0.0%st
    private void parseSunCpuMetrics(String rec, DBRec dbRec) throws Exception {

        int tokCnt = 0;

        if (rec.length() > 0) {

            StringTokenizer st = new StringTokenizer(rec);
            while (st.hasMoreTokens()) {
                String tok = st.nextToken();
                tokCnt++;
                DBAttributes dbAttribs = null;
                switch (tokCnt) {
                case 3:
                    //trim the % sign
                    tok = tok.substring(0, tok.length() - 1);
                    dbAttribs = new DBAttributes("idle", tok);
                    dbRec.addAttrib(dbAttribs);
                    break;
                case 5:
                    //trim the % sign
                    tok = tok.substring(0, tok.length() - 1);
                    dbAttribs = new DBAttributes("user", tok);
                    dbRec.addAttrib(dbAttribs);
                    break;
                case 7:
                    //trim the % sign
                    tok = tok.substring(0, tok.length() - 1);
                    dbAttribs = new DBAttributes("kernel", tok);
                    dbRec.addAttrib(dbAttribs);
                    break;
                case 9:
                    //trim the % sign
                    tok = tok.substring(0, tok.length() - 1);
                    dbAttribs = new DBAttributes("iowait", tok);
                    dbRec.addAttrib(dbAttribs);
                    break;
                case 11:
                    //trim the % sign
                    tok = tok.substring(0, tok.length() - 1);
                    dbAttribs = new DBAttributes("swap", tok);
                    dbRec.addAttrib(dbAttribs);
                    break;
                default:
                    break;
                }
            }
        }
    }

    //SUNOS:  CPU states: 94.5% idle,  4.6% user,  0.9% kernel,  0.0% iowait,  0.0% swap
    //LINUX:  Cpu(s):  5.0%us,  2.5%sy,  0.0%ni, 92.4%id,  0.0%wa,  0.0%hi,  0.0%si,  0.0%st
    private void parseLinuxCpuMetrics(String rec, DBRec dbRec) throws Exception {

        int tokCnt = 0;

        if (rec.length() > 0) {

            StringTokenizer st = new StringTokenizer(rec);
            while (st.hasMoreTokens()) {
                String tok = st.nextToken();
                tokCnt++;
                DBAttributes dbAttribs = null;
                int pos = 0;
                String metricVal = "";
                String metricName = "";
                switch (tokCnt) {
                case 2: case 3: case 4: case 5: case 6: case 7: case 8:
                    //trim the % sign
                    pos = tok.indexOf('%');
                    metricVal = tok.substring(1, pos);
                    metricName = tok.substring(pos+1, tok.length()-1);
                    dbAttribs = new DBAttributes(metricName, metricVal);
                    dbRec.addAttrib(dbAttribs);
                    break;
                case 9:
                        //trim the % sign
                    pos = tok.indexOf('%');
                    metricVal = tok.substring(1, pos);
                    metricName = tok.substring(pos+1, tok.length());
                    dbAttribs = new DBAttributes(metricName, metricVal);
                    dbRec.addAttrib(dbAttribs);
                    break;
                default:
                    break;
                }
            }
        }
    }

    //SUNOS:  Memory: 255G phys mem, 141G free mem, 48G total swap, 48G free swap
    //LINUX:  Mem:  16465924k total, 16155248k used,   310676k free,   594528k buffers
    private void parseSunMemoryMetrics(String rec, DBRec dbRec) throws Exception {
        int tokCnt = 0;

        if (rec.length() > 0) {

            StringTokenizer st = new StringTokenizer(rec);
            while (st.hasMoreTokens()) {
                String tok = st.nextToken();
                tokCnt++;
                DBAttributes dbAttribs = null;
                switch (tokCnt) {
                case 2:
                    //trim the G symbol
                    tok = tok.substring(0, tok.length() - 1);
                    dbAttribs = new DBAttributes("PhysMem", tok);
                    dbRec.addAttrib(dbAttribs);
                    break;
                case 5:
                    //trim the G symbol
                    tok = tok.substring(0, tok.length() - 1);
                    dbAttribs = new DBAttributes("FreeMem", tok);
                    dbRec.addAttrib(dbAttribs);
                    break;
                case 8:
                    //trim the G symbol
                    tok = tok.substring(0, tok.length() - 1);
                    dbAttribs = new DBAttributes("TotalSwap", tok);
                    dbRec.addAttrib(dbAttribs);
                    break;
                case 11:
                    //trim the G symbol
                    tok = tok.substring(0, tok.length() - 1);
                    dbAttribs = new DBAttributes("FreeSwap", tok);
                    dbRec.addAttrib(dbAttribs);
                    break;
                default:
                    break;
                }
            }
        }
    }
    //SUNOS:  Memory: 255G phys mem, 141G free mem, 48G total swap, 48G free swap
    //LINUX:  Mem:  16465924k total, 16155248k used,   310676k free,   594528k buffers
    private void parseLinuxMemoryMetrics(String rec, DBRec dbRec) throws Exception {
        int tokCnt = 0;

        if (rec.length() > 0) {

            StringTokenizer st = new StringTokenizer(rec);
            while (st.hasMoreTokens()) {
                String tok = st.nextToken();
                tokCnt++;
                DBAttributes dbAttribs = null;
                switch (tokCnt) {
                case 2:
                    //trim the K symbol
                    tok = tok.substring(0, tok.length() - 1);
                    dbAttribs = new DBAttributes("total", tok);
                    dbRec.addAttrib(dbAttribs);
                    break;
                case 4:
                    //trim the K symbol
                    tok = tok.substring(0, tok.length() - 1);
                    dbAttribs = new DBAttributes("used", tok);
                    dbRec.addAttrib(dbAttribs);
                    break;
                case 6:
                    //trim the K symbol
                    tok = tok.substring(0, tok.length() - 1);
                    dbAttribs = new DBAttributes("free", tok);
                    dbRec.addAttrib(dbAttribs);
                    break;
                case 8:
                    //trim the K symbol
                    tok = tok.substring(0, tok.length() - 1);
                    dbAttribs = new DBAttributes("buffers", tok);
                    dbRec.addAttrib(dbAttribs);
                    break;
                default:
                    break;
                }
            }
        }
    }

}
